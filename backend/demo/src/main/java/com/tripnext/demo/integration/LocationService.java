package com.tripnext.demo.integration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.tripnext.demo.dto.external.PlaceResult;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * Place search for destinations and activity locations (SRS FR-EXT-001).
 *
 * Uses Google Places (Text Search, New) when GOOGLE_MAPS_API_KEY is set,
 * otherwise OpenStreetMap Nominatim, which needs no key (SRS CON-SCOPE-003).
 * Results are cached in Redis for 24 h. Nominatim's usage policy asks for a
 * descriptive User-Agent, caching, and at most ~1 request/second; the cache
 * and the frontend's debounce keep us well under that.
 *
 * Any provider failure returns an empty list: the itinerary still works with
 * a manually typed location (SRS 7.11).
 */
@Slf4j
@Service
public class LocationService {

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int LIMIT = 6;

    private final StringRedisTemplate redis;
    private final String googleKey;
    private final String userAgent;
    private final RestClient nominatim = HttpClients.restClient("https://nominatim.openstreetmap.org");
    private final RestClient google = HttpClients.restClient("https://places.googleapis.com");

    public LocationService(
            StringRedisTemplate redis,
            @Value("${app.places.google-api-key:}") String googleKey,
            @Value("${app.places.user-agent}") String userAgent
    ) {
        this.redis = redis;
        this.googleKey = googleKey;
        this.userAgent = userAgent;
    }

    public List<PlaceResult> search(String query) {
        String q = query.trim().replaceAll("\\s+", " ");
        String cacheKey = "places:" + provider() + ":" + q.toLowerCase(Locale.ROOT);

        List<PlaceResult> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            List<PlaceResult> results = googleKey.isBlank() ? searchNominatim(q) : searchGoogle(q);
            writeCache(cacheKey, results);
            return results;
        } catch (Exception e) {
            log.warn("Place search via {} failed for '{}': {}", provider(), q, e.getMessage());
            return List.of();
        }
    }

    private String provider() {
        return googleKey.isBlank() ? "osm" : "google";
    }

    private List<PlaceResult> searchNominatim(String q) {
        String body = nominatim.get()
                .uri(u -> u.path("/search")
                        .queryParam("q", q)
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", LIMIT)
                        .build())
                .header("User-Agent", userAgent)
                .header("Accept-Language", "en")
                .retrieve()
                .body(String.class);

        JsonNode arr = HttpClients.JSON.readTree(body == null ? "[]" : body);
        List<JsonNode> nodes = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            nodes.add(arr.get(i));
        }
        // Most important first, so "Goa India" finds the Indian state, not a
        // shop of that name; the first result is what the weather card uses.
        nodes.sort((a, b) -> Double.compare(b.path("importance").asDouble(), a.path("importance").asDouble()));

        List<PlaceResult> out = new ArrayList<>();
        for (JsonNode n : nodes) {
            String display = n.path("display_name").asString("");
            String name = n.path("name").asString("");
            out.add(new PlaceResult(
                    name.isBlank() ? display.split(",")[0] : name,
                    display,
                    n.path("lat").asDouble(),
                    n.path("lon").asDouble()
            ));
        }
        return out;
    }

    private List<PlaceResult> searchGoogle(String q) {
        String body = google.post()
                .uri("/v1/places:searchText")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", googleKey)
                .header("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.location")
                .body(HttpClients.JSON.writeValueAsString(Map.of("textQuery", q, "pageSize", LIMIT)))
                .retrieve()
                .body(String.class);

        JsonNode places = HttpClients.JSON.readTree(body == null ? "{}" : body).path("places");
        List<PlaceResult> out = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            JsonNode p = places.get(i);
            out.add(new PlaceResult(
                    p.path("displayName").path("text").asString(""),
                    p.path("formattedAddress").asString(""),
                    p.path("location").path("latitude").asDouble(),
                    p.path("location").path("longitude").asDouble()
            ));
        }
        return out;
    }

    private List<PlaceResult> readCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : Arrays.asList(HttpClients.JSON.readValue(json, PlaceResult[].class));
        } catch (Exception e) {
            return null; // Redis down or bad entry: just ask the provider
        }
    }

    private void writeCache(String key, List<PlaceResult> results) {
        try {
            redis.opsForValue().set(key, HttpClients.JSON.writeValueAsString(results), CACHE_TTL);
        } catch (Exception e) {
            log.debug("Could not cache {}: {}", key, e.getMessage());
        }
    }
}
