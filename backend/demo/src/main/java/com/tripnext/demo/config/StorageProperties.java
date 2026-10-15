package com.tripnext.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * S3-compatible object storage. Locally this points at SeaweedFS; on AWS leave
 * endpoint/public-endpoint/access-key/secret-key empty to use real S3 with
 * the default credentials chain (IAM role).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    // Endpoint the backend talks to, e.g. http://s3:8333 inside docker-compose.
    private String endpoint;

    // Endpoint browsers use for presigned URLs, e.g. http://localhost:8333.
    // Falls back to endpoint when empty.
    private String publicEndpoint;

    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;

    // SeaweedFS/MinIO need path-style URLs (http://host/bucket/key).
    private boolean pathStyle = true;

    // Pre-signed GET (download) and PUT (upload) lifetimes; the SRS asks for 15 minutes.
    private long urlExpiryMinutes = 15;
    private long uploadUrlExpiryMinutes = 15;

    private Bucket bucket = new Bucket();

    @Getter
    @Setter
    public static class Bucket {
        private String documents;
        private String media;
        private String wallet;
    }
}
