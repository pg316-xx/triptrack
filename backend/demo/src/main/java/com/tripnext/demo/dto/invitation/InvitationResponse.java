package com.tripnext.demo.dto.invitation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitationResponse(
        UUID invitationId,
        String inviteLink,
        OffsetDateTime expiresAt
) {}