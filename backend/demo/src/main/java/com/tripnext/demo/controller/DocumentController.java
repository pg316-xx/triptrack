package com.tripnext.demo.controller;

import java.util.List;
import java.util.UUID;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tripnext.demo.dto.document.CommitDocumentRequest;
import com.tripnext.demo.dto.document.CommitMediaRequest;
import com.tripnext.demo.dto.document.DocumentResponse;
import com.tripnext.demo.dto.document.TripMediaResponse;
import com.tripnext.demo.dto.document.UpdateMediaRequest;
import com.tripnext.demo.dto.document.UploadUrlRequest;
import com.tripnext.demo.model.help.DocType;
import com.tripnext.demo.model.help.MediaFileType;
import com.tripnext.demo.service.FileService;
import com.tripnext.demo.service.storage.PresignedUpload;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips/{tripId}")
@RequiredArgsConstructor
public class DocumentController {
    private final FileService fileService;

// ---- Direct-to-storage uploads: get a URL, PUT the file there, then commit ----
@PostMapping("/documents/upload-url")
public ResponseEntity<PresignedUpload> documentUploadUrl(
        @PathVariable UUID tripId,
        @RequestBody UploadUrlRequest request
) {
    return ResponseEntity.ok(
            fileService.requestDocumentUpload(tripId, request)
    );
}
@PostMapping(
        value = "/documents",
        consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
)
public ResponseEntity<DocumentResponse> commitDocument(
        @PathVariable UUID tripId,
        @RequestBody CommitDocumentRequest request
) {
    return ResponseEntity.status(201).body(
            fileService.commitDocument(tripId, request)
    );
}
@PostMapping("/media/upload-url")
public ResponseEntity<PresignedUpload> mediaUploadUrl(
        @PathVariable UUID tripId,
        @RequestBody UploadUrlRequest request
) {
    return ResponseEntity.ok(
            fileService.requestMediaUpload(tripId, request)
    );
}
@PostMapping(
        value = "/media",
        consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
)
public ResponseEntity<TripMediaResponse> commitMedia(
        @PathVariable UUID tripId,
        @RequestBody CommitMediaRequest request
) {
    return ResponseEntity.status(201).body(
            fileService.commitMedia(tripId, request)
    );
}

// ---- Upload through the backend (multipart); kept for scripts/tools ----
@PostMapping(
        value = "/documents",
        consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
)
public ResponseEntity<DocumentResponse> uploadDocument(
        @PathVariable UUID tripId,
        @RequestParam("file") MultipartFile file,
        @RequestParam DocType documentType
) {

    return ResponseEntity.ok(
            fileService.uploadDocument(
                    tripId,
                    file,
                    documentType
            )
    );
}
@PostMapping(
        value = "/media",
        consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
)
public ResponseEntity<TripMediaResponse> uploadMedia(
        @PathVariable UUID tripId,
        @RequestParam("file") MultipartFile file,
        @RequestParam MediaFileType mediaFileType,
        @RequestParam(required = false) String caption
) {

    return ResponseEntity.ok(
            fileService.uploadMedia(
                    tripId,
                    file,
                    mediaFileType,
                    caption
            )
    );
}
@GetMapping("/documents")
public ResponseEntity<List<DocumentResponse>>
getDocuments(
        @PathVariable UUID tripId
) {

    return ResponseEntity.ok(
            fileService.getDocuments(
                    tripId
            )
    );
}
// ?page=0&size=24, newest first
@GetMapping("/media")
public ResponseEntity<Page<TripMediaResponse>>
getMedia(
        @PathVariable UUID tripId,
        @PageableDefault(size = 24) Pageable pageable
) {

    return ResponseEntity.ok(
            fileService.getMedia(
                    tripId,
                    pageable
            )
    );
}
@PatchMapping("/media/{mediaId}")
public ResponseEntity<TripMediaResponse> updateMedia(
        @PathVariable UUID mediaId,
        @RequestBody UpdateMediaRequest request
) {
    return ResponseEntity.ok(
            fileService.updateMediaCaption(mediaId, request)
    );
}
@DeleteMapping("/documents/{documentId}")
public ResponseEntity<Void> deleteDocument(
        @PathVariable UUID documentId
) {

    fileService.deleteDocument(
            documentId
    );

    return ResponseEntity.noContent()
            .build();
}
@DeleteMapping("/media/{mediaId}")
public ResponseEntity<Void> deleteMedia(
        @PathVariable UUID mediaId
) {

    fileService.deleteMedia(
            mediaId
    );

    return ResponseEntity.noContent()
            .build();
}
@GetMapping("/documents/{documentId}")
public ResponseEntity<DocumentResponse> getDocument(
        @PathVariable UUID documentId
) {
    return ResponseEntity.ok(
            fileService.getDocument(
                    documentId
            )
    );
}
@GetMapping("/media/{mediaId}")
public ResponseEntity<TripMediaResponse> getMediaById(
        @PathVariable UUID mediaId
) {
    return ResponseEntity.ok(
            fileService.getMediaById(
                    mediaId
            )
    );
}

}


