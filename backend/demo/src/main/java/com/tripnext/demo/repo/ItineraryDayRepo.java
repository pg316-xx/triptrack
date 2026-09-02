package com.tripnext.demo.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


import com.tripnext.demo.model.ItineraryDay;
import com.tripnext.demo.model.Trip;

public interface ItineraryDayRepo extends JpaRepository<ItineraryDay, UUID> {
    List<ItineraryDay> findByTripOrderByDayNumber(
            Trip trip
    );
    boolean existsByTripAndDayNumber(
        Trip trip,
        Integer dayNumber
);

}
