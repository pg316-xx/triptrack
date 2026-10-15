package com.tripnext.demo.service.storage;

import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores files by object key. Callers persist the key and ask for a
 * short-lived URL whenever they hand the file to a client.
 */
public interface FileStorageService {

    // Server-side upload (multipart requests that go through the backend).
    String upload(
            StorageBucket bucket,
            MultipartFile file,
            String key
    );

    // Phase 1 of a direct upload: a short-lived URL the browser PUTs the file to.
    PresignedUpload presignUpload(
            StorageBucket bucket,
            String key,
            String contentType
    );

    // Phase 2 check: does the object exist, and how big is it really?
    Optional<StoredObject> find(
            StorageBucket bucket,
            String key
    );

    String getDownloadUrl(
            StorageBucket bucket,
            String key
    );

    void delete(
            StorageBucket bucket,
            String key
    );
}
