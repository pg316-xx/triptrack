package com.tripnext.demo.service.storage;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import okhttp3.Response;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
@Service
@RequiredArgsConstructor
public class SupabaseStorageService implements FileStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket.documents}")
    private String documentsBucket;

    @Value("${supabase.bucket.media}")
    private String mediaBucket;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public String uploadDocument(
            MultipartFile file,
            String path
    ) {
        return upload(
                file,
                documentsBucket,
                path
        );
    }

    @Override
    public String uploadMedia(
            MultipartFile file,
            String path
    ) {
        return upload(
                file,
                mediaBucket,
                path
        );
    }

    private String upload(
            MultipartFile file,
            String bucket,
            String path
    ) {

        try {

            RequestBody body =
                    RequestBody.create(
                            file.getBytes(),
                            okhttp3.MediaType.parse(
                                    file.getContentType()
                            )
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    supabaseUrl
                                    + "/storage/v1/object/"
                                    + bucket
                                    + "/"
                                    + path
                            )
                            .addHeader(
                                    "Authorization",
                                    "Bearer " + serviceKey
                            )
                            .addHeader(
                                    "apikey",
                                    serviceKey
                            )
                            .put(body)
                            .build();

            try (Response response =
                         client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    throw new RuntimeException(
                            "Upload failed: "
                            + response.code()
                    );
                }
            }

            return supabaseUrl
                    + "/storage/v1/object/public/"
                    + bucket
                    + "/"
                    + path;

        } catch (Exception e) {
            throw new RuntimeException(
                    "File upload failed",
                    e
            );
        }
    }

    @Override
    public void deleteFile(
            String fileUrl
    ) {
        try {
            String basePath = supabaseUrl + "/storage/v1/object/public/";
            if (!fileUrl.startsWith(basePath)) {
                return; 
            }

            String bucketAndPath = fileUrl.substring(basePath.length()); 

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/storage/v1/object/" + bucketAndPath)
                    .addHeader("Authorization", "Bearer " + serviceKey)
                    .addHeader("apikey", serviceKey)
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Failed to delete file from Supabase: " + response.code());
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting file from Supabase: " + e.getMessage());
        }
    }
}