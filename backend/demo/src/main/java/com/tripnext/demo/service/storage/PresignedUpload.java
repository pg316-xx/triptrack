package com.tripnext.demo.service.storage;

import java.time.OffsetDateTime;

/**
 * A pre-signed PUT the browser uses to upload straight to storage.
 * The client must send the Content-Type header exactly as given.
 */
public record PresignedUpload(
        String uploadUrl,
        String storageKey,
        String contentType,
        OffsetDateTime expiresAt
) {}
