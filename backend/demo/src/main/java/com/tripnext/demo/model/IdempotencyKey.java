package com.tripnext.demo.model;



import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String key;

    @Column(nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private String operationType;

    public IdempotencyKey(
            String key,
            UUID resourceId,
            String operationType
    ) {
        this.key = key;
        this.resourceId = resourceId;
        this.operationType = operationType;
    }
}