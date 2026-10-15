package com.tripnext.demo.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.TripMemberRole;

public interface TripMemberRepo extends JpaRepository<TripMember, UUID>{
    List<TripMember> findByUser(User user);
    boolean existsByTripAndUser(
        Trip trip,
        User user
);
boolean existsByTripAndUserAndRole(
        Trip trip,
        User user,
        TripMemberRole role
);
void deleteByTrip(Trip trip);
List<TripMember> findByTrip(Trip trip);
Optional<TripMember> findByTripAndUser(
        Trip trip,
        User user
);
boolean existsByTrip_IdAndUser_Id(
        UUID tripId,
        UUID userId
);
}
