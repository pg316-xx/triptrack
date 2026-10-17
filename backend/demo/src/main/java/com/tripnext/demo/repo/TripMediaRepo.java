package com.tripnext.demo.repo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripMedia;

public interface TripMediaRepo extends JpaRepository<TripMedia, UUID>{
    List<TripMedia> findByTripOrderByCreatedAtDesc(
            Trip trip
    );
    Page<TripMedia> findByTripOrderByCreatedAtDesc(
            Trip trip,
            Pageable pageable
    );
    Optional<TripMedia> findByIdAndTrip(
        UUID id,
        Trip trip
);
    boolean existsByStorageKey(String storageKey);
}
