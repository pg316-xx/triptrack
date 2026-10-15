package com.tripnext.demo.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(
        StorageProperties.class
)
public class StorageConfig {

    @Bean
    S3Client s3Client(StorageProperties props) {
        var builder = S3Client.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(credentials(props))
                .forcePathStyle(props.isPathStyle())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);

        if (StringUtils.hasText(props.getEndpoint())) {
            builder.endpointOverride(URI.create(props.getEndpoint()));
        }
        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner(StorageProperties props) {
        var builder = S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(props.isPathStyle())
                                .build()
                );

        String endpoint = StringUtils.hasText(props.getPublicEndpoint())
                ? props.getPublicEndpoint()
                : props.getEndpoint();
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private AwsCredentialsProvider credentials(StorageProperties props) {
        if (StringUtils.hasText(props.getAccessKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
            );
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
