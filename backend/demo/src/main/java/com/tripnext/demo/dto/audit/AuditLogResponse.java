package com.tripnext.demo.dto.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String userName,
        String action,
        String entityType,
        UUID entityId,
        String metadata,
        OffsetDateTime createdAt
) {
}
