package com.tripnext.demo.service;

import java.util.List;
import java.util.UUID;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tripnext.demo.dto.document.DocumentResponse;
import com.tripnext.demo.dto.document.TripMediaResponse;
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

    String fileUrl =
            fileStorageService.uploadDocument(
                    file,
                    path
            );

    Document document =
            new Document(
                    trip,
                    currentUser,
                    file.getOriginalFilename(),
                    fileUrl,
                    documentType,
                    file.getSize()
            );
  
    Document saved =
            documentRepo.save(document);

    return new DocumentResponse(
            saved.getId(),
            saved.getFileName(),
            saved.getFileUrl(),
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

    String fileUrl =
            fileStorageService.uploadMedia(
                    file,
                    path
            );

    TripMedia media =
            new TripMedia(
                    trip,
                    currentUser,
                    fileUrl,
                    mediaFileType,
                    caption
            );

    TripMedia saved =
            tripMediaRepo.save(media);

    return new TripMediaResponse(
            saved.getId(),
            saved.getFileUrl(),
            saved.getMediaFileType(),
            saved.getCaption(),
            saved.getCreatedAt()
    );
}
    @Cacheable(value = "tripDocuments", key = "#tripId")
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
                    document.getFileUrl(),
                    document.getDocumentType(),
                    document.getFileSizeBytes(),
                    document.getCreatedAt()
            ))
            .toList();
}
    @Cacheable(value = "tripMedia", key = "#tripId")
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
                    media.getFileUrl(),
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
    fileStorageService.deleteFile(
            document.getFileUrl()
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
    fileStorageService.deleteFile(
            media.getFileUrl()
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

    return new DocumentResponse(
            document.getId(),
            document.getFileName(),
            document.getFileUrl(),
            document.getDocumentType(),
            document.getFileSizeBytes(),
            document.getCreatedAt()
    );
}
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

    return new TripMediaResponse(
            media.getId(),
            media.getFileUrl(),
            media.getMediaFileType(),
            media.getCaption(),
            media.getCreatedAt()
    );
}
}
