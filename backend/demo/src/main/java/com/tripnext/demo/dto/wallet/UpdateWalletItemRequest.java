package com.tripnext.demo.dto.wallet;

import java.util.UUID;

import com.tripnext.demo.model.help.WalletItemType;

/**
 * All fields are applied as sent, so send the item's full metadata:
 * a null tripId unlinks it (and un-shares it).
 */
public record UpdateWalletItemRequest(
        String title,
        WalletItemType itemType,
        UUID tripId,
        Boolean shared
) {}
