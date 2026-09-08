package com.tripnext.demo.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.MediaFileType;


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
@Table(name = "trip_media")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripMedia {

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
            name = "file_url",
            nullable = false,
            length = 1000
    )
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "media_type",
            nullable = false
    )
    private MediaFileType mediaFileType;

    @Column(length = 500)
    private String caption;

    @Column(
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    public TripMedia(
            Trip trip,
            User uploadedBy,
            String fileUrl,
            MediaFileType mediaFileType,
            String caption
    ) {
        this.trip = trip;
        this.uploadedBy = uploadedBy;
        this.fileUrl = fileUrl;
        this.mediaFileType = mediaFileType;
        this.caption = caption;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}