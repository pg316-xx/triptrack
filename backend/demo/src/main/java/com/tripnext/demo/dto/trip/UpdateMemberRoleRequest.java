package com.tripnext.demo.dto.trip;

import com.tripnext.demo.model.help.TripMemberRole;

public record UpdateMemberRoleRequest(
        TripMemberRole role
) {}