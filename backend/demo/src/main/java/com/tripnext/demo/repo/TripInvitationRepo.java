package com.tripnext.demo.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripInvitation;

public interface TripInvitationRepo extends JpaRepository<TripInvitation, UUID>{
     Optional<TripInvitation>
        findByTokenHash(String tokenHash);
        Optional<TripInvitation> findById(UUID id);
        List<TripInvitation> findByTrip(
        Trip trip
);
}
