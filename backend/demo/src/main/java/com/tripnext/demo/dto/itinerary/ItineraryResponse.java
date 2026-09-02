package com.tripnext.demo.dto.itinerary;

import java.util.List;
import java.util.UUID;

public record ItineraryResponse(
        UUID tripId,
        List<ItineraryDayDetailResponse> days
) {}