package com.tripnext.demo.dto.itinerary;

import java.time.LocalDate;

public record CreateItineraryDayRequest(
        Integer dayNumber,
        LocalDate date,
        String title,
        String notes
) {}