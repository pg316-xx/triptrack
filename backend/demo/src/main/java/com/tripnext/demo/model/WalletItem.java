package com.tripnext.demo.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.WalletItemType;

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

/**
 * A private travel document owned by one user. Optionally linked to a trip;
 * other trip members can see it only when {@code shared} is true.
 */
@Entity
@Table(name = "wallet_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    // Nullable; the DB sets it to NULL if the trip is deleted.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "item_type",
            nullable = false,
            length = 30
    )
    private WalletItemType itemType;

    @Column(
            name = "storage_key",
            nullable = false,
            length = 500,
            unique = true
    )
    private String storageKey;

    @Column(
            name = "file_name",
            nullable = false,
            length = 200
    )
    private String fileName;

    @Column(
            name = "content_type",
            nullable = false,
            length = 100
    )
    private String contentType;

    @Column(
            name = "file_size_bytes",
            nullable = false
    )
    private Long fileSizeBytes;

    @Column(
            name = "is_shared",
            nullable = false
    )
    private boolean shared;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    public WalletItem(
            User user,
            Trip trip,
            String title,
            WalletItemType itemType,
            String storageKey,
            String fileName,
            String contentType,
            Long fileSizeBytes,
            boolean shared
    ) {
        this.user = user;
        this.trip = trip;
        this.title = title;
        this.itemType = itemType;
        this.storageKey = storageKey;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.shared = shared;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
