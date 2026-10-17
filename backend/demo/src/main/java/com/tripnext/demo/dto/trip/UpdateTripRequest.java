package com.tripnext.demo.dto.trip;

import java.time.LocalDate;

/** Partial update: null fields are left unchanged. Send coverImageUrl "" to remove the cover. */
public record UpdateTripRequest(
        String title,
        String description,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String coverImageUrl
) {}
