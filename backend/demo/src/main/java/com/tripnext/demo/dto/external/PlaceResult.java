package com.tripnext.demo.dto.external;

/** A place from location search (SRS FR-EXT-001), normalized across providers. */
public record PlaceResult(
        String name,
        String address,
        double latitude,
        double longitude
) {}
