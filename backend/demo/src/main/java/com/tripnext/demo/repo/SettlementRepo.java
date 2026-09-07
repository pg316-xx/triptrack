package com.tripnext.demo.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.Settlement;
import com.tripnext.demo.model.Trip;

public interface SettlementRepo extends JpaRepository<Settlement, UUID>{
    List<Settlement> findByTrip(Trip trip);
    List<Settlement> findByTripOrderByCreatedAtDesc(
        Trip trip
);
}
