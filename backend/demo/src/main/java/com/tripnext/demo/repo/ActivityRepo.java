package com.tripnext.demo.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


import com.tripnext.demo.model.Activity;
import com.tripnext.demo.model.ItineraryDay;


public interface ActivityRepo extends JpaRepository<Activity, UUID> {
           List<Activity>
    findByItineraryDayOrderByOrderIndex(
            ItineraryDay itineraryDay
    );
     

    boolean existsByItineraryDayAndOrderIndex(
            ItineraryDay itineraryDay,
            Integer orderIndex
    );
}