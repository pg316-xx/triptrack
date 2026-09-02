package com.tripnext.demo.dto.trip;

import java.util.UUID;

import com.tripnext.demo.model.help.TripMemberRole;

public record TripMemberResponse(
        UUID userId,
        String name,
        String email,
        TripMemberRole role
) {}