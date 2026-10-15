package com.tripnext.demo.service.storage;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tripnext.demo.config.StorageProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Works against AWS S3 and any S3-compatible store (SeaweedFS locally).
 * Buckets are private; clients get pre-signed PUT/GET URLs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements FileStorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties props;

    @Override
    public String upload(StorageBucket bucket, MultipartFile file, String key) {
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(name(bucket))
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return key;
        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public PresignedUpload presignUpload(StorageBucket bucket, String key, String contentType) {
        Duration ttl = Duration.ofMinutes(props.getUploadUrlExpiryMinutes());
        String url = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .putObjectRequest(
                                PutObjectRequest.builder()
                                        .bucket(name(bucket))
                                        .key(key)
                                        .contentType(contentType)
                                        .build()
                        )
                        .build()
        ).url().toString();

        return new PresignedUpload(url, key, contentType, OffsetDateTime.now().plus(ttl));
    }

    @Override
    public Optional<StoredObject> find(StorageBucket bucket, String key) {
        try {
            HeadObjectResponse head = s3.headObject(
                    HeadObjectRequest.builder()
                            .bucket(name(bucket))
                            .key(key)
                            .build()
            );
            return Optional.of(new StoredObject(head.contentLength(), head.contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public String getDownloadUrl(StorageBucket bucket, String key) {
        return presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(props.getUrlExpiryMinutes()))
                        .getObjectRequest(
                                GetObjectRequest.builder()
                                        .bucket(name(bucket))
                                        .key(key)
                                        .build()
                        )
                        .build()
        ).url().toString();
    }

    @Override
    public void delete(StorageBucket bucket, String key) {
        try {
            s3.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(name(bucket))
                            .key(key)
                            .build()
            );
        } catch (Exception e) {
            // Don't block removing the DB row if the object is already gone.
            log.warn("Failed to delete {}/{} from storage: {}", name(bucket), key, e.getMessage());
        }
    }

    private String name(StorageBucket bucket) {
        return switch (bucket) {
            case DOCUMENTS -> props.getBucket().getDocuments();
            case MEDIA -> props.getBucket().getMedia();
            case WALLET -> props.getBucket().getWallet();
        };
    }
}
