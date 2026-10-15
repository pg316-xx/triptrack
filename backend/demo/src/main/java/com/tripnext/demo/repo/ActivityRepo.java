package com.tripnext.demo.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


import com.tripnext.demo.model.Activity;
import com.tripnext.demo.model.ItineraryDay;


public interface ActivityRepo extends JpaRepository<Activity, UUID> {
           List<Activity>
    findByItineraryDayOrderByOrderIndex(
            ItineraryDay itineraryDay
    );

    // Last activity of a day (for appending).
    Optional<Activity> findFirstByItineraryDayOrderByOrderIndexDesc(
            ItineraryDay itineraryDay
    );

    // First activity of a day (for moving to the top).
    Optional<Activity> findFirstByItineraryDayOrderByOrderIndexAsc(
            ItineraryDay itineraryDay
    );

    // The activity directly after a given rank, skipping the one being moved.
    Optional<Activity> findFirstByItineraryDayAndOrderIndexGreaterThanAndIdNotOrderByOrderIndexAsc(
            ItineraryDay itineraryDay,
            String orderIndex,
            UUID excludedId
    );
}
