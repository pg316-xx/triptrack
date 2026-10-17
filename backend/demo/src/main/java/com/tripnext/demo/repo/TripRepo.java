package com.tripnext.demo.repo;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.User;

public interface TripRepo extends JpaRepository<Trip, UUID> {

    // Trips the user belongs to; the Pageable's sort applies to Trip fields (e.g. startDate).
    @Query("select t from Trip t where exists (select 1 from TripMember m where m.trip = t and m.user = :user)")
    Page<Trip> findTripsForUser(@Param("user") User user, Pageable pageable);
}
