package com.tripnext.demo.dto.trip;

import java.time.LocalDate;

public record UpdateTripRequest(

        String title,

        String description,

        String destination,

        LocalDate startDate,

        LocalDate endDate

) {}