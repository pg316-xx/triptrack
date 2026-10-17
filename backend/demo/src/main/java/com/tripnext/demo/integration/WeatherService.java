package com.tripnext.demo.integration;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.tripnext.demo.dto.external.WeatherResponse;
import com.tripnext.demo.dto.external.WeatherResponse.Current;
import com.tripnext.demo.dto.external.WeatherResponse.Day;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * Current weather + 5-day forecast (SRS FR-EXT-002).
 *
 * Uses OpenWeatherMap when OPENWEATHER_API_KEY is set, otherwise Open-Meteo,
 * which needs no key. Cached in Redis for 3 h under
 * weather:forecast:{lat}:{lon} (coordinates rounded to 2 decimals, ~1 km).
 *
 * Graceful degradation (SRS 7.11): if the provider fails, the last good
 * forecast (kept 7 days) is returned marked stale; if there is none, an
 * "unavailable" response is returned instead of an error.
 */
@Slf4j
@Service
public class WeatherService {

    private static final Duration FRESH_TTL = Duration.ofHours(3);
    private static final Duration STALE_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final String openWeatherKey;
    private final RestClient openMeteo = HttpClients.restClient("https://api.open-meteo.com");
    private final RestClient openWeather = HttpClients.restClient("https://api.openweathermap.org");

    public WeatherService(
            StringRedisTemplate redis,
            @Value("${app.weather.openweather-api-key:}") String openWeatherKey
    ) {
        this.redis = redis;
        this.openWeatherKey = openWeatherKey;
    }

    public WeatherResponse forecast(double lat, double lon) {
        double rLat = Math.round(lat * 100) / 100.0;
        double rLon = Math.round(lon * 100) / 100.0;
        String coords = String.format(Locale.ROOT, "%.2f:%.2f", rLat, rLon);
        String freshKey = "weather:forecast:" + coords;
        String staleKey = "weather:last:" + coords;

        WeatherResponse cached = read(freshKey);
        if (cached != null) {
            return cached;
        }

        try {
            WeatherResponse fresh = openWeatherKey.isBlank() ? fromOpenMeteo(rLat, rLon) : fromOpenWeather(rLat, rLon);
            write(freshKey, fresh, FRESH_TTL);
            write(staleKey, fresh, STALE_TTL);
            return fresh;
        } catch (Exception e) {
            log.warn("Weather provider failed for {}: {}", coords, e.getMessage());
            WeatherResponse last = read(staleKey);
            if (last != null) {
                return new WeatherResponse(true, true, last.provider(), last.latitude(), last.longitude(),
                        last.fetchedAt(), last.current(), last.daily());
            }
            return WeatherResponse.unavailable(rLat, rLon);
        }
    }

    private WeatherResponse fromOpenMeteo(double lat, double lon) {
        String body = openMeteo.get()
                .uri(u -> u.path("/v1/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("current", "temperature_2m,weather_code,wind_speed_10m")
                        .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                        .queryParam("forecast_days", 5)
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .body(String.class);

        JsonNode root = HttpClients.JSON.readTree(body);
        JsonNode cur = root.path("current");
        int curCode = cur.path("weather_code").asInt();
        Current current = new Current(
                cur.path("temperature_2m").asDouble(),
                wmoCondition(curCode),
                wmoIcon(curCode),
                cur.path("wind_speed_10m").asDouble() // km/h
        );

        JsonNode d = root.path("daily");
        List<Day> days = new ArrayList<>();
        for (int i = 0; i < d.path("time").size(); i++) {
            int code = d.path("weather_code").get(i).asInt();
            JsonNode pop = d.path("precipitation_probability_max").path(i);
            days.add(new Day(
                    d.path("time").get(i).asString(),
                    d.path("temperature_2m_min").get(i).asDouble(),
                    d.path("temperature_2m_max").get(i).asDouble(),
                    wmoCondition(code),
                    wmoIcon(code),
                    pop.isMissingNode() || pop.isNull() ? null : pop.asInt()
            ));
        }
        return new WeatherResponse(true, false, "open-meteo", lat, lon, OffsetDateTime.now().toString(), current, days);
    }

    private WeatherResponse fromOpenWeather(double lat, double lon) {
        String body = openWeather.get()
                .uri(u -> u.path("/data/2.5/forecast")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("units", "metric")
                        .queryParam("appid", openWeatherKey)
                        .build())
                .retrieve()
                .body(String.class);

        JsonNode list = HttpClients.JSON.readTree(body).path("list");
        if (list.size() == 0) {
            throw new IllegalStateException("empty forecast");
        }

        JsonNode first = list.get(0);
        int firstId = first.path("weather").path(0).path("id").asInt();
        Current current = new Current(
                first.path("main").path("temp").asDouble(),
                capitalize(first.path("weather").path(0).path("description").asString("")),
                owIcon(firstId),
                first.path("wind").path("speed").asDouble() * 3.6 // m/s -> km/h
        );

        // 3-hourly entries -> per-day min/max, condition at midday, max rain chance
        Map<String, double[]> minMax = new LinkedHashMap<>();
        Map<String, Integer> idAt = new LinkedHashMap<>();
        Map<String, Integer> pop = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            JsonNode e = list.get(i);
            String date = e.path("dt_txt").asString("").split(" ")[0];
            double t = e.path("main").path("temp").asDouble();
            minMax.merge(date, new double[] {t, t}, (a, b) -> new double[] {Math.min(a[0], b[0]), Math.max(a[1], b[1])});
            int id = e.path("weather").path(0).path("id").asInt();
            if (!idAt.containsKey(date) || e.path("dt_txt").asString("").contains("12:00")) {
                idAt.put(date, id);
            }
            pop.merge(date, (int) Math.round(e.path("pop").asDouble() * 100), Math::max);
        }

        List<Day> days = new ArrayList<>();
        for (String date : minMax.keySet()) {
            if (days.size() == 5) {
                break;
            }
            int id = idAt.get(date);
            days.add(new Day(date, minMax.get(date)[0], minMax.get(date)[1], owCondition(id), owIcon(id), pop.get(date)));
        }
        return new WeatherResponse(true, false, "openweathermap", lat, lon, OffsetDateTime.now().toString(), current, days);
    }

    // WMO weather interpretation codes (Open-Meteo)
    private static String wmoCondition(int c) {
        if (c == 0) return "Clear sky";
        if (c == 1) return "Mainly clear";
        if (c == 2) return "Partly cloudy";
        if (c == 3) return "Overcast";
        if (c == 45 || c == 48) return "Fog";
        if (c >= 51 && c <= 57) return "Drizzle";
        if (c >= 61 && c <= 67) return "Rain";
        if (c >= 71 && c <= 77) return "Snow";
        if (c >= 80 && c <= 82) return "Rain showers";
        if (c == 85 || c == 86) return "Snow showers";
        if (c >= 95) return "Thunderstorm";
        return "Unknown";
    }

    private static String wmoIcon(int c) {
        if (c == 0 || c == 1) return "sun";
        if (c == 2) return "cloud-sun";
        if (c == 3) return "cloud";
        if (c == 45 || c == 48) return "fog";
        if (c >= 51 && c <= 57) return "drizzle";
        if ((c >= 61 && c <= 67) || (c >= 80 && c <= 82)) return "rain";
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return "snow";
        if (c >= 95) return "storm";
        return "cloud";
    }

    // OpenWeatherMap condition ids
    private static String owIcon(int id) {
        if (id >= 200 && id < 300) return "storm";
        if (id >= 300 && id < 400) return "drizzle";
        if (id >= 500 && id < 600) return "rain";
        if (id >= 600 && id < 700) return "snow";
        if (id >= 700 && id < 800) return "fog";
        if (id == 800) return "sun";
        if (id == 801 || id == 802) return "cloud-sun";
        return "cloud";
    }

    private static String owCondition(int id) {
        return switch (owIcon(id)) {
            case "storm" -> "Thunderstorm";
            case "drizzle" -> "Drizzle";
            case "rain" -> "Rain";
            case "snow" -> "Snow";
            case "fog" -> "Fog";
            case "sun" -> "Clear sky";
            case "cloud-sun" -> "Partly cloudy";
            default -> "Cloudy";
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private WeatherResponse read(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : HttpClients.JSON.readValue(json, WeatherResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void write(String key, WeatherResponse value, Duration ttl) {
        try {
            redis.opsForValue().set(key, HttpClients.JSON.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.debug("Could not cache {}: {}", key, e.getMessage());
        }
    }
}
