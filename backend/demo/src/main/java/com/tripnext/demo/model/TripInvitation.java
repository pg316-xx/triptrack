package com.tripnext.demo.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.TripRole;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip_invitations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripInvitation {

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
            name = "invited_by",
            nullable = false
    )
    private User invitedBy;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 255
    )
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private TripRole role;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private OffsetDateTime expiresAt;

    @Column(
            name = "usage_limit",
            nullable = false
    )
    private Integer usageLimit;

    @Column(
            name = "usage_count",
            nullable = false
    )
    private Integer usageCount;

    @Column(
            name = "is_revoked",
            nullable = false
    )
    private Boolean isRevoked;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    public TripInvitation(
            Trip trip,
            User invitedBy,
            String tokenHash,
            TripRole role,
            OffsetDateTime expiresAt,
            Integer usageLimit
    ) {
        this.trip = trip;
        this.invitedBy = invitedBy;
        this.tokenHash = tokenHash;
        this.role = role;
        this.expiresAt = expiresAt;
        this.usageLimit = usageLimit;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.usageCount = 0;
        this.isRevoked = false;
    }
}