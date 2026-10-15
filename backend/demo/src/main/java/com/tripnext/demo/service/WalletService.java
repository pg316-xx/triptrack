package com.tripnext.demo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.document.UploadUrlRequest;
import com.tripnext.demo.dto.wallet.CreateWalletItemRequest;
import com.tripnext.demo.dto.wallet.UpdateWalletItemRequest;
import com.tripnext.demo.dto.wallet.WalletItemResponse;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.WalletItem;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;
import com.tripnext.demo.repo.WalletItemRepo;
import com.tripnext.demo.service.storage.FileStorageService;
import com.tripnext.demo.service.storage.PresignedUpload;
import com.tripnext.demo.service.storage.StorageBucket;
import com.tripnext.demo.service.storage.StoredObject;
import com.tripnext.demo.util.StorageKeys;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Personal Travel Wallet (SRS FR-S3-002, CON-PRIV-001/002).
 *
 * Items belong to one user. Trip membership never grants access by itself:
 * another member can see an item only if its owner linked it to that trip
 * and marked it shared. Items others can't see answer 404, not 403, so their
 * existence isn't revealed.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletItemRepo walletItemRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;

    public PresignedUpload requestUpload(UploadUrlRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        if (request.sizeBytes() == null) {
            throw new BadRequestException("sizeBytes is required");
        }
        fileValidator.validateWalletFile(request.contentType(), request.sizeBytes());

        return fileStorageService.presignUpload(
                StorageBucket.WALLET,
                StorageKeys.newKey(currentUser.getId(), request.fileName()),
                request.contentType()
        );
    }

    @Transactional
    public WalletItemResponse create(CreateWalletItemRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        validateMetadata(request.title(), request.itemType());

        String key = request.storageKey();
        // Keys are issued as "{userId}/...": you can only commit your own uploads.
        if (key == null
                || !key.startsWith(currentUser.getId() + "/")
                || key.contains("..")) {
            throw new BadRequestException("Invalid storageKey");
        }
        if (walletItemRepo.existsByStorageKey(key)) {
            throw new BadRequestException("This upload was already saved");
        }

        StoredObject stored = fileStorageService.find(StorageBucket.WALLET, key)
                .orElseThrow(() -> new BadRequestException(
                        "File not found in storage; upload it to the pre-signed URL first"
                ));
        try {
            fileValidator.validateWalletFile(stored.contentType(), stored.sizeBytes());
        } catch (BadRequestException e) {
            fileStorageService.delete(StorageBucket.WALLET, key);
            throw e;
        }

        Trip trip = linkedTrip(request.tripId(), currentUser);
        boolean shared = Boolean.TRUE.equals(request.shared());
        if (shared && trip == null) {
            throw new BadRequestException("Link the item to a trip to share it");
        }

        String fileName = request.fileName() == null || request.fileName().isBlank()
                ? key.substring(key.lastIndexOf('/') + 1)
                : request.fileName();

        WalletItem saved = walletItemRepo.save(
                new WalletItem(
                        currentUser,
                        trip,
                        request.title().trim(),
                        request.itemType(),
                        key,
                        fileName.length() > 200 ? fileName.substring(0, 200) : fileName,
                        stored.contentType(),
                        stored.sizeBytes(),
                        shared
                )
        );
        return toResponse(saved);
    }

    @Transactional
    public List<WalletItemResponse> listMine() {
        User currentUser = currentUserService.getCurrentUser();
        return walletItemRepo.findByUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WalletItemResponse get(UUID itemId) {
        User currentUser = currentUserService.getCurrentUser();
        WalletItem item = findItem(itemId);

        boolean isOwner = item.getUser().getId().equals(currentUser.getId());
        boolean sharedWithMe = item.isShared()
                && item.getTrip() != null
                && tripMemberRepo.existsByTripAndUser(item.getTrip(), currentUser);

        if (!isOwner && !sharedWithMe) {
            throw new ResourceNotFoundException("Wallet item not found");
        }
        return toResponse(item);
    }

    @Transactional
    public WalletItemResponse update(UUID itemId, UpdateWalletItemRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        WalletItem item = findOwnedItem(itemId, currentUser);

        validateMetadata(request.title(), request.itemType());

        Trip trip = linkedTrip(request.tripId(), currentUser);
        boolean shared = Boolean.TRUE.equals(request.shared());
        if (shared && trip == null) {
            throw new BadRequestException("Link the item to a trip to share it");
        }

        item.setTitle(request.title().trim());
        item.setItemType(request.itemType());
        item.setTrip(trip);
        item.setShared(shared);

        return toResponse(walletItemRepo.save(item));
    }

    @Transactional
    public void delete(UUID itemId) {
        User currentUser = currentUserService.getCurrentUser();
        WalletItem item = findOwnedItem(itemId, currentUser);

        fileStorageService.delete(StorageBucket.WALLET, item.getStorageKey());
        walletItemRepo.delete(item);
    }

    // Items members of this trip explicitly shared into it.
    @Transactional
    public List<WalletItemResponse> listSharedForTrip(UUID tripId) {
        User currentUser = currentUserService.getCurrentUser();
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (!tripMemberRepo.existsByTripAndUser(trip, currentUser)) {
            throw new UnauthorizedException("You are not a member of this trip");
        }

        return walletItemRepo.findByTripAndSharedTrueOrderByCreatedAtDesc(trip)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private WalletItem findItem(UUID itemId) {
        return walletItemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet item not found"));
    }

    private WalletItem findOwnedItem(UUID itemId, User user) {
        WalletItem item = findItem(itemId);
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Wallet item not found");
        }
        return item;
    }

    // You can only link an item to a trip you belong to.
    private Trip linkedTrip(UUID tripId, User user) {
        if (tripId == null) {
            return null;
        }
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        if (!tripMemberRepo.existsByTripAndUser(trip, user)) {
            throw new UnauthorizedException("You are not a member of this trip");
        }
        return trip;
    }

    private void validateMetadata(String title, Object itemType) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (title.trim().length() > 200) {
            throw new BadRequestException("Title is too long");
        }
        if (itemType == null) {
            throw new BadRequestException("itemType is required");
        }
    }

    private WalletItemResponse toResponse(WalletItem item) {
        return new WalletItemResponse(
                item.getId(),
                item.getTitle(),
                item.getItemType(),
                item.getFileName(),
                item.getContentType(),
                item.getFileSizeBytes(),
                item.getTrip() == null ? null : item.getTrip().getId(),
                item.isShared(),
                item.getUser().getId(),
                item.getUser().getName(),
                fileStorageService.getDownloadUrl(StorageBucket.WALLET, item.getStorageKey()),
                item.getCreatedAt()
        );
    }
}
