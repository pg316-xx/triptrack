package com.tripnext.demo.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadDocument(
            MultipartFile file,
            String path
    );

    String uploadMedia(
            MultipartFile file,
            String path
    );

    void deleteFile(
            String fileUrl
    );
}