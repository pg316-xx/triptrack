package com.tripnext.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.external.PlaceResult;
import com.tripnext.demo.dto.external.WeatherResponse;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.integration.LocationService;
import com.tripnext.demo.integration.WeatherService;

import lombok.RequiredArgsConstructor;

/** Proxies for external data (SRS 8.2.10). Keys stay server-side; responses are cached in Redis. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExternalDataController {

    private final LocationService locationService;
    private final WeatherService weatherService;

    @GetMapping("/locations/search")
    public ResponseEntity<List<PlaceResult>> searchLocations(@RequestParam("q") String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2 || query.length() > 120) {
            throw new BadRequestException("Search text must be 2-120 characters");
        }
        return ResponseEntity.ok(locationService.search(query));
    }

    @GetMapping("/weather")
    public ResponseEntity<WeatherResponse> weather(@RequestParam("lat") double lat, @RequestParam("lon") double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new BadRequestException("lat must be -90..90 and lon -180..180");
        }
        return ResponseEntity.ok(weatherService.forecast(lat, lon));
    }
}
