package com.tripnext.demo.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.ExpenseCategory;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

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
            name = "paid_by",
            nullable = false
    )
    private User paidBy;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Expense(
            Trip trip,
            User paidBy,
            BigDecimal amount,
            String title,
            String description,
            ExpenseCategory category
    ) {
        this.trip = trip;
        this.paidBy = paidBy;
        this.amount = amount;
        this.title = title;
        this.description = description;
        this.category = category;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
