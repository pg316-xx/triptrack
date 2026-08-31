package com.tripnext.demo.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expense_splits")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expensesplit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "expense_id",
            nullable = false
    )
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(name = "split_amount", nullable = false)
    private BigDecimal splitAmount;

    public Expensesplit(
            Expense expense,
            User user,
            BigDecimal splitAmount
    ) {
        this.expense = expense;
        this.user = user;
        this.splitAmount = splitAmount;
    }
}