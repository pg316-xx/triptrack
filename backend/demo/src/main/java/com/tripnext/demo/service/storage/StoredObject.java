package com.tripnext.demo.service.storage;

/** What storage reports about an uploaded object (used to verify direct uploads). */
public record StoredObject(
        long sizeBytes,
        String contentType
) {}
