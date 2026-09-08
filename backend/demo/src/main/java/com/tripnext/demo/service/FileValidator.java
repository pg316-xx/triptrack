package com.tripnext.demo.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileValidator {
    public void validateDocument(
        MultipartFile file
) {

    String contentType =
            file.getContentType();
    if (file.getSize() > 10 * 1024 * 1024) {
    throw new IllegalArgumentException(
            "Document exceeds 10 MB"
    );
}
    if (!List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    ).contains(contentType)) {

        throw new IllegalArgumentException(
                "Unsupported document type"
        );
    }
}
public void validateMedia(
        MultipartFile file
) {

    String contentType =
            file.getContentType();
    if (file.getSize() > 50 * 1024 * 1024) {
    throw new IllegalArgumentException(
            "Media exceeds 50 MB"
    );
}
    if (!contentType.startsWith("image/")
            && !contentType.startsWith("video/")) {

        throw new IllegalArgumentException(
                "Unsupported media type"
        );
    }
}
}
