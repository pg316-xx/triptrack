package com.tripnext.demo.dto.itinerary;

import java.time.LocalDate;
import java.util.UUID;

public record ItineraryDayResponse(
        UUID id,
        Integer dayNumber,
        LocalDate date,
        String title,
        String notes
) {}