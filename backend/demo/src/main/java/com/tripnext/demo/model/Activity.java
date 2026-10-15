package com.tripnext.demo.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity

@Table(name = "activities")

@Getter

@Setter

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "itinerary_day_id",
            nullable = false
    )
    private ItineraryDay itineraryDay;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 1000)
    private String description;
    @Column(length = 200)
    private String location;
    @Column
    private OffsetDateTime startTime;
    @Column
    private OffsetDateTime endTime;
    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedCost;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(
        name = "created_by",
        nullable = false
)
private User createdBy;

@Column
private Double latitude;

@Column
private Double longitude;

@Column(name = "order_index", nullable = false)
private String orderIndex;

@Version
private Integer version;
    public Activity(
        ItineraryDay itineraryDay,
        User createdBy,
        String title,
        String description,
        String location,
        Double latitude,
        Double longitude,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal estimatedCost,
        String orderIndex
) {
    this.itineraryDay = itineraryDay;
    this.createdBy = createdBy;
    this.title = title;
    this.description = description;
    this.location = location;
    this.latitude = latitude;
    this.longitude = longitude;
    this.startTime = startTime;
    this.endTime = endTime;
    this.estimatedCost = estimatedCost;
    this.orderIndex = orderIndex;
}
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
