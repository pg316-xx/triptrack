package com.tripnext.demo.dto.wallet;

import java.util.UUID;

import com.tripnext.demo.model.help.WalletItemType;

/**
 * Commit a wallet file already uploaded to its pre-signed URL.
 *
 * @param tripId optional trip to link the item to
 * @param shared show it to that trip's members (requires tripId)
 */
public record CreateWalletItemRequest(
        String title,
        WalletItemType itemType,
        String storageKey,
        String fileName,
        UUID tripId,
        Boolean shared
) {}
