package com.tripnext.demo.dto.document;

/** Phase 1 of a direct upload: describe the file so the backend can authorize it. */
public record UploadUrlRequest(
        String fileName,
        String contentType,
        Long sizeBytes
) {}
