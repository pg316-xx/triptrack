package com.tripnext.demo.dto.itinerary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdateActivityRequest(
        String title,
        String description,
        String location,
        Double latitude,
        Double longitude,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal estimatedCost,
        Integer version
) {}
