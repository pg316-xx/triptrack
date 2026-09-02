package com.tripnext.demo.dto.invitation;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.TripRole;

public record InvitationSummaryResponse(
        UUID invitationId,
        TripRole role,
        Integer usageLimit,
        Integer usageCount,
        Boolean revoked,
        OffsetDateTime expiresAt
) {}