package com.tripnext.demo.repo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


import com.tripnext.demo.model.Document;
import com.tripnext.demo.model.Trip;

public interface DocumentRepo extends JpaRepository<Document, UUID> {
     List<Document> findByTripOrderByCreatedAtDesc(
            Trip trip
    );
    Optional<Document> findByIdAndTrip(
        UUID id,
        Trip trip
);
    boolean existsByStorageKey(String storageKey);
}
