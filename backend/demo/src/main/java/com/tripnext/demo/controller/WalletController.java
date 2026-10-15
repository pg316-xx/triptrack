package com.tripnext.demo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.document.UploadUrlRequest;
import com.tripnext.demo.dto.wallet.CreateWalletItemRequest;
import com.tripnext.demo.dto.wallet.UpdateWalletItemRequest;
import com.tripnext.demo.dto.wallet.WalletItemResponse;
import com.tripnext.demo.service.WalletService;
import com.tripnext.demo.service.storage.PresignedUpload;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<List<WalletItemResponse>> myWallet() {
        return ResponseEntity.ok(walletService.listMine());
    }

    // Phase 1: get a pre-signed URL, then PUT the file to it.
    @PostMapping("/wallet/items/upload-url")
    public ResponseEntity<PresignedUpload> uploadUrl(
            @RequestBody UploadUrlRequest request
    ) {
        return ResponseEntity.ok(walletService.requestUpload(request));
    }

    // Phase 2: commit the uploaded file as a wallet item.
    @PostMapping("/wallet/items")
    public ResponseEntity<WalletItemResponse> create(
            @RequestBody CreateWalletItemRequest request
    ) {
        return ResponseEntity.status(201).body(walletService.create(request));
    }

    @GetMapping("/wallet/items/{itemId}")
    public ResponseEntity<WalletItemResponse> get(
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(walletService.get(itemId));
    }

    @PatchMapping("/wallet/items/{itemId}")
    public ResponseEntity<WalletItemResponse> update(
            @PathVariable UUID itemId,
            @RequestBody UpdateWalletItemRequest request
    ) {
        return ResponseEntity.ok(walletService.update(itemId, request));
    }

    @DeleteMapping("/wallet/items/{itemId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID itemId
    ) {
        walletService.delete(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trips/{tripId}/wallet-items")
    public ResponseEntity<List<WalletItemResponse>> sharedWithTrip(
            @PathVariable UUID tripId
    ) {
        return ResponseEntity.ok(walletService.listSharedForTrip(tripId));
    }
}
