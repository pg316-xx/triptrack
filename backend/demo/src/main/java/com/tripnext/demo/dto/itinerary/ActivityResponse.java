package com.tripnext.demo.dto.itinerary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        String title,
        String description,
        String location,
        Double latitude,
        Double longitude,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal estimatedCost,
        Integer orderIndex,
        Integer version
) {}