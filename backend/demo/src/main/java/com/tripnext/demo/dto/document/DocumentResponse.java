package com.tripnext.demo.dto.document;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.DocType;

public record DocumentResponse(
        UUID id,
        String fileName,
        String fileUrl,
        DocType documentType,
        Long fileSizeBytes,
        OffsetDateTime createdAt
) {}