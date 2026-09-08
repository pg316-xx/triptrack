package com.tripnext.demo.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.DocType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "trip_id",
            nullable = false
    )
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "uploaded_by_id",
            nullable = false
    )
    private User uploadedBy;

    @Column(
            name = "file_name",
            nullable = false,
            length = 200
    )
    private String fileName;

    @Column(
            name = "file_url",
            nullable = false,
            length = 500
    )
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "document_type",
            nullable = false,
            length = 50
    )
    private DocType documentType;

    @Column(
            name = "file_size_bytes",
            nullable = false
    )
    private Long fileSizeBytes;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    public Document(
            Trip trip,
            User uploadedBy,
            String fileName,
            String fileUrl,
            DocType documentType,
            Long fileSizeBytes
    ) {
        this.trip = trip;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.documentType = documentType;
        this.fileSizeBytes = fileSizeBytes;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}