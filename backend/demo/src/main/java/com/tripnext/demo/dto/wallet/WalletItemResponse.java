package com.tripnext.demo.dto.wallet;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.WalletItemType;

/** fileUrl is a short-lived pre-signed download link. */
public record WalletItemResponse(
        UUID id,
        String title,
        WalletItemType itemType,
        String fileName,
        String contentType,
        Long fileSizeBytes,
        UUID tripId,
        boolean shared,
        UUID ownerId,
        String ownerName,
        String fileUrl,
        OffsetDateTime createdAt
) {}
