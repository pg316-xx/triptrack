package com.tripnext.demo.dto.itinerary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// New activities are appended to the end of the day; use the move endpoint to reorder.
public record CreateActivityRequest(
        String title,
        String description,
        String location,
        Double latitude,
        Double longitude,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal estimatedCost
) {}
