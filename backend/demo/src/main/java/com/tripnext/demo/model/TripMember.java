package com.tripnext.demo.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.TripMemberRole;

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
@Table(name = "trip_members")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripMember {

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
            name = "user_id",
            nullable = false
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripMemberRole role;

    @Column(nullable = false)
    private OffsetDateTime joinedAt;

    public TripMember(
            Trip trip,
            User user,
            TripMemberRole role
    ) {
        this.trip = trip;
        this.user = user;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        this.joinedAt = OffsetDateTime.now();
    }
}