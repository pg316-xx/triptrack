package com.tripnext.demo.dto.trip;

import java.time.LocalDate;
import java.util.UUID;

public record TripResponse(

        UUID id,

        String name,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        String createdByName

) {}