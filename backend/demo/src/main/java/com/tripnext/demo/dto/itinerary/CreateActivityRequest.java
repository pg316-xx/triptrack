package com.tripnext.demo.dto.itinerary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateActivityRequest(
        String title,
        String description,
        String location,
        Double latitude,
        Double longitude,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal estimatedCost,
        Integer orderIndex
) {}