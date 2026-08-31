package com.tripnext.demo.model;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity

@Table(name = "itinerary_days")

@Getter

@Setter

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryDay {
    @Id

    @GeneratedValue(strategy = GenerationType.UUID)

    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(

            name = "trip_id",

            nullable = false

    )
    private Trip trip;
    @Column(nullable = false)
    private Integer dayNumber;
    @Column(nullable = false, length = 200)
    private String title;

   @Column(length = 1000)
    private String notes;
    @Column(nullable = false, updatable = false)

    private OffsetDateTime createdAt;
    public ItineraryDay(Trip trip, Integer dayNumber, String title, String notes) {
        this.trip = trip;
        this.dayNumber = dayNumber;
        this.title = title;
        this.notes = notes;
    }
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
