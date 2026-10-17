package com.tripnext.demo.dto.external;

import java.util.List;

/**
 * Weather normalized across providers (SRS FR-EXT-002).
 *
 * @param available false when no provider answered and nothing was cached
 * @param stale     true when served from an older cached copy because the provider failed
 */
public record WeatherResponse(
        boolean available,
        boolean stale,
        String provider,
        double latitude,
        double longitude,
        String fetchedAt,
        Current current,
        List<Day> daily
) {
    public record Current(double temperatureC, String condition, String icon, double windKph) {}

    public record Day(String date, double minC, double maxC, String condition, String icon, Integer precipitationChance) {}

    public static WeatherResponse unavailable(double lat, double lon) {
        return new WeatherResponse(false, false, null, lat, lon, null, null, List.of());
    }
}
