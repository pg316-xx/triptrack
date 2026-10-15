package com.tripnext.demo.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.tripnext.demo.exception.BadRequestException;

/**
 * Type and size rules for uploads. Each rule takes (contentType, size) so the
 * same checks run when a pre-signed upload URL is requested and again when the
 * upload is committed (against what storage actually received).
 */
@Component
public class FileValidator {

    private static final long DOCUMENT_MAX_BYTES = 10L * 1024 * 1024;
    private static final long MEDIA_MAX_BYTES = 50L * 1024 * 1024;

    private static final List<String> DOCUMENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public void validateDocument(MultipartFile file) {
        validateDocument(file.getContentType(), file.getSize());
    }

    public void validateMedia(MultipartFile file) {
        validateMedia(file.getContentType(), file.getSize());
    }

    public void validateDocument(String contentType, long sizeBytes) {
        checkSize(sizeBytes, DOCUMENT_MAX_BYTES, "Document exceeds 10 MB");
        if (!DOCUMENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported document type");
        }
    }

    public void validateMedia(String contentType, long sizeBytes) {
        checkSize(sizeBytes, MEDIA_MAX_BYTES, "Media exceeds 50 MB");
        if (contentType == null
                || (!contentType.startsWith("image/")
                        && !contentType.startsWith("video/"))) {
            throw new BadRequestException("Unsupported media type");
        }
    }

    // Wallet items are personal documents (tickets, confirmations, IDs).
    public void validateWalletFile(String contentType, long sizeBytes) {
        validateDocument(contentType, sizeBytes);
    }

    private void checkSize(long sizeBytes, long max, String message) {
        if (sizeBytes <= 0) {
            throw new BadRequestException("File is empty");
        }
        if (sizeBytes > max) {
            throw new BadRequestException(message);
        }
    }
}
