package com.tripnext.demo.dto.itinerary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ItineraryDayDetailResponse(
        UUID id,
        Integer dayNumber,
        LocalDate date,
        String title,
        String notes,
        List<ActivitySummaryResponse> activities
) {}