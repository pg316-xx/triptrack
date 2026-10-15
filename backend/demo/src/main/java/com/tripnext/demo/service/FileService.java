package com.tripnext.demo.service;

import java.util.List;
import java.util.UUID;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tripnext.demo.dto.document.CommitDocumentRequest;
import com.tripnext.demo.dto.document.CommitMediaRequest;
import com.tripnext.demo.dto.document.DocumentResponse;
import com.tripnext.demo.dto.document.TripMediaResponse;
import com.tripnext.demo.dto.document.UploadUrlRequest;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.Document;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripMedia;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.DocType;
import com.tripnext.demo.model.help.MediaFileType;
import com.tripnext.demo.model.help.TripMemberRole;
import com.tripnext.demo.repo.DocumentRepo;
import com.tripnext.demo.repo.TripMediaRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;
import com.tripnext.demo.service.storage.FileStorageService;
import com.tripnext.demo.service.storage.PresignedUpload;
import com.tripnext.demo.service.storage.StorageBucket;
import com.tripnext.demo.service.storage.StoredObject;
import com.tripnext.demo.util.StorageKeys;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final DocumentRepo documentRepo;
    private final TripMediaRepo tripMediaRepo;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final AuditLogService auditLogService;
    @Transactional
    @CacheEvict(value = "tripDocuments", key = "#tripId")
public DocumentResponse uploadDocument(
        UUID tripId,
        MultipartFile file,
        DocType documentType
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip =
        getTripAndValidateMembership(
                tripId
        );
    
    
    fileValidator.validateDocument(file);

    String path =
            tripId + "/"
            + UUID.randomUUID()
            + "-"
            + file.getOriginalFilename();

    String storageKey =
            fileStorageService.upload(
                    StorageBucket.DOCUMENTS,
                    file,
                    path
            );

    Document document =
            new Document(
                    trip,
                    currentUser,
                    file.getOriginalFilename(),
                    storageKey,
                    documentType,
                    file.getSize()
            );
  
    Document saved =
            documentRepo.save(document);

    return new DocumentResponse(
            saved.getId(),
            saved.getFileName(),
            fileStorageService.getDownloadUrl(StorageBucket.DOCUMENTS, saved.getStorageKey()),
            saved.getDocumentType(),
            saved.getFileSizeBytes(),
            saved.getCreatedAt()
    );
}

   @Transactional
   @CacheEvict(value = "tripMedia", key = "#tripId")
public TripMediaResponse uploadMedia(
        UUID tripId,
        MultipartFile file,
        MediaFileType mediaFileType,
        String caption
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip =
        getTripAndValidateMembership(
                tripId
        );

    
    fileValidator.validateMedia(file);
    

    String path =
            tripId + "/"
            + UUID.randomUUID()
            + "-"
            + file.getOriginalFilename();

    String storageKey =
            fileStorageService.upload(
                    StorageBucket.MEDIA,
                    file,
                    path
            );

    TripMedia media =
            new TripMedia(
                    trip,
                    currentUser,
                    storageKey,
                    mediaFileType,
                    caption
            );

    TripMedia saved =
            tripMediaRepo.save(media);

    return new TripMediaResponse(
            saved.getId(),
            fileStorageService.getDownloadUrl(StorageBucket.MEDIA, saved.getStorageKey()),
            saved.getMediaFileType(),
            saved.getCaption(),
            saved.getCreatedAt()
    );
}
    // Not cached: cache hits skipped the membership check and records with OffsetDateTime don't round-trip through the Redis JSON serializer.
@Transactional
public List<DocumentResponse> getDocuments(
        UUID tripId
) {

    Trip trip =
        getTripAndValidateMembership(
                tripId
        );

    return documentRepo
            .findByTripOrderByCreatedAtDesc(trip)
            .stream()
            .map(document -> new DocumentResponse(
                    document.getId(),
                    document.getFileName(),
                    fileStorageService.getDownloadUrl(StorageBucket.DOCUMENTS, document.getStorageKey()),
                    document.getDocumentType(),
                    document.getFileSizeBytes(),
                    document.getCreatedAt()
            ))
            .toList();
}
    // Not cached: cache hits skipped the membership check and records with OffsetDateTime don't round-trip through the Redis JSON serializer.
@Transactional
public List<TripMediaResponse> getMedia(
        UUID tripId
) {

    Trip trip =
        getTripAndValidateMembership(
                tripId
        );

    return tripMediaRepo
            .findByTripOrderByCreatedAtDesc(trip)
            .stream()
            .map(media -> new TripMediaResponse(
                    media.getId(),
                    fileStorageService.getDownloadUrl(StorageBucket.MEDIA, media.getStorageKey()),
                    media.getMediaFileType(),
                    media.getCaption(),
                    media.getCreatedAt()
            ))
            .toList();
}
@Transactional
@CacheEvict(value = "tripDocuments", allEntries = true)
public void deleteDocument(
        UUID documentId
) {

    Document document =
            documentRepo.findById(documentId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Document not found"
                            )
                    );
    
    Trip trip = document.getTrip();
validateCanManageFiles(trip);
    fileStorageService.delete(
            StorageBucket.DOCUMENTS,
            document.getStorageKey()
    );

    documentRepo.delete(document);

    auditLogService.logAction(
            trip,
            "DELETE",
            "Document",
            documentId,
            "Deleted document: " + document.getFileName()
    );
}
@Transactional
@CacheEvict(value = "tripMedia", allEntries = true)
public void deleteMedia(
        UUID mediaId
) {

    TripMedia media =
            tripMediaRepo.findById(mediaId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Media not found"
                            )
                    );
    Trip trip = media.getTrip();
   validateCanManageFiles(trip);
    fileStorageService.delete(
            StorageBucket.MEDIA,
            media.getStorageKey()
    );

    tripMediaRepo.delete(media);

    auditLogService.logAction(
            trip,
            "DELETE",
            "Media",
            mediaId,
            "Deleted media file"
    );
}
private Trip getTripAndValidateMembership(
        UUID tripId
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isMember =
            tripMemberRepo.existsByTripAndUser(
                    trip,
                    currentUser
            );

    if (!isMember) {
        throw new UnauthorizedException(
                "Not a trip member"
        );
    }

    return trip;
}
private void validateCanManageFiles(
        Trip trip
) {

    User currentUser =
            currentUserService.getCurrentUser();

    TripMember member =
            tripMemberRepo.findByTripAndUser(
                    trip,
                    currentUser
            )
            .orElseThrow();

    if (member.getRole() != TripMemberRole.OWNER
            && member.getRole() != TripMemberRole.ADMIN) {

        throw new UnauthorizedException(
                "Insufficient permissions"
        );
    }
}
@Transactional
public DocumentResponse getDocument(
        UUID documentId
) {
    Document document =
            documentRepo.findById(documentId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Document not found"
                            )
                    );

    getTripAndValidateMembership(
            document.getTrip().getId()
    );

    return new DocumentResponse(
            document.getId(),
            document.getFileName(),
            fileStorageService.getDownloadUrl(StorageBucket.DOCUMENTS, document.getStorageKey()),
            document.getDocumentType(),
            document.getFileSizeBytes(),
            document.getCreatedAt()
    );
}
@Transactional
public TripMediaResponse getMediaById(
        UUID mediaId
) {
    TripMedia media =
            tripMediaRepo.findById(mediaId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Media not found"
                            )
                    );

    getTripAndValidateMembership(
            media.getTrip().getId()
    );

    return new TripMediaResponse(
            media.getId(),
            fileStorageService.getDownloadUrl(StorageBucket.MEDIA, media.getStorageKey()),
            media.getMediaFileType(),
            media.getCaption(),
            media.getCreatedAt()
    );
}

    // ---- Two-phase direct upload (SRS 11.4) ----
    // 1. Client asks for a pre-signed PUT URL (we check membership + type/size).
    // 2. Client PUTs the file straight to storage.
    // 3. Client commits; we confirm the object really exists and re-check the
    //    size/type storage reports before saving any metadata.

    public PresignedUpload requestDocumentUpload(
            UUID tripId,
            UploadUrlRequest request
    ) {
        getTripAndValidateMembership(tripId);
        fileValidator.validateDocument(request.contentType(), sizeOf(request));

        return fileStorageService.presignUpload(
                StorageBucket.DOCUMENTS,
                StorageKeys.newKey(tripId, request.fileName()),
                request.contentType()
        );
    }

    public PresignedUpload requestMediaUpload(
            UUID tripId,
            UploadUrlRequest request
    ) {
        getTripAndValidateMembership(tripId);
        fileValidator.validateMedia(request.contentType(), sizeOf(request));

        return fileStorageService.presignUpload(
                StorageBucket.MEDIA,
                StorageKeys.newKey(tripId, request.fileName()),
                request.contentType()
        );
    }

    @Transactional
    public DocumentResponse commitDocument(
            UUID tripId,
            CommitDocumentRequest request
    ) {
        User currentUser = currentUserService.getCurrentUser();
        Trip trip = getTripAndValidateMembership(tripId);

        if (request.documentType() == null) {
            throw new BadRequestException("documentType is required");
        }
        checkKeyBelongsToTrip(request.storageKey(), tripId);
        if (documentRepo.existsByStorageKey(request.storageKey())) {
            throw new BadRequestException("This upload was already saved");
        }

        StoredObject stored = requireUploaded(StorageBucket.DOCUMENTS, request.storageKey());
        try {
            fileValidator.validateDocument(stored.contentType(), stored.sizeBytes());
        } catch (BadRequestException e) {
            // Don't keep a file that bypassed the limits we signed for.
            fileStorageService.delete(StorageBucket.DOCUMENTS, request.storageKey());
            throw e;
        }

        Document saved = documentRepo.save(
                new Document(
                        trip,
                        currentUser,
                        fileNameOrDefault(request.fileName(), request.storageKey()),
                        request.storageKey(),
                        request.documentType(),
                        stored.sizeBytes()
                )
        );

        return new DocumentResponse(
                saved.getId(),
                saved.getFileName(),
                fileStorageService.getDownloadUrl(StorageBucket.DOCUMENTS, saved.getStorageKey()),
                saved.getDocumentType(),
                saved.getFileSizeBytes(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public TripMediaResponse commitMedia(
            UUID tripId,
            CommitMediaRequest request
    ) {
        User currentUser = currentUserService.getCurrentUser();
        Trip trip = getTripAndValidateMembership(tripId);

        if (request.mediaFileType() == null) {
            throw new BadRequestException("mediaFileType is required");
        }
        checkKeyBelongsToTrip(request.storageKey(), tripId);
        if (tripMediaRepo.existsByStorageKey(request.storageKey())) {
            throw new BadRequestException("This upload was already saved");
        }

        StoredObject stored = requireUploaded(StorageBucket.MEDIA, request.storageKey());
        try {
            fileValidator.validateMedia(stored.contentType(), stored.sizeBytes());
        } catch (BadRequestException e) {
            fileStorageService.delete(StorageBucket.MEDIA, request.storageKey());
            throw e;
        }

        TripMedia saved = tripMediaRepo.save(
                new TripMedia(
                        trip,
                        currentUser,
                        request.storageKey(),
                        request.mediaFileType(),
                        request.caption()
                )
        );

        return new TripMediaResponse(
                saved.getId(),
                fileStorageService.getDownloadUrl(StorageBucket.MEDIA, saved.getStorageKey()),
                saved.getMediaFileType(),
                saved.getCaption(),
                saved.getCreatedAt()
        );
    }

    private long sizeOf(UploadUrlRequest request) {
        if (request.sizeBytes() == null) {
            throw new BadRequestException("sizeBytes is required");
        }
        return request.sizeBytes();
    }

    // Keys are issued as "{tripId}/..."; refuse keys for another trip's files.
    private void checkKeyBelongsToTrip(String storageKey, UUID tripId) {
        if (storageKey == null
                || !storageKey.startsWith(tripId + "/")
                || storageKey.contains("..")) {
            throw new BadRequestException("Invalid storageKey for this trip");
        }
    }

    private StoredObject requireUploaded(StorageBucket bucket, String storageKey) {
        return fileStorageService.find(bucket, storageKey)
                .orElseThrow(() -> new BadRequestException(
                        "File not found in storage; upload it to the pre-signed URL first"
                ));
    }

    private String fileNameOrDefault(String fileName, String storageKey) {
        if (fileName != null && !fileName.isBlank()) {
            return fileName.length() > 200 ? fileName.substring(0, 200) : fileName;
        }
        return storageKey.substring(storageKey.lastIndexOf('/') + 1);
    }
}
