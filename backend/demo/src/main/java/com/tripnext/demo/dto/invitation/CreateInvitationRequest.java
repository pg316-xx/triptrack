package com.tripnext.demo.dto.invitation;

import com.tripnext.demo.model.help.TripRole;

public record CreateInvitationRequest(
        TripRole role,
        Integer usageLimit
) {}