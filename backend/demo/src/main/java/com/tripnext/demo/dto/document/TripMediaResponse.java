package com.tripnext.demo.dto.document;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.MediaFileType;

public record TripMediaResponse(
        UUID id,
        String fileUrl,
        MediaFileType mediaType,
        String caption,
        OffsetDateTime createdAt,
        UUID uploadedByUserId,
        String uploadedByName
) {}
