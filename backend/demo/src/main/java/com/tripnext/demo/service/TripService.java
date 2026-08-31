package com.tripnext.demo.service;

import java.util.List;
import java.util.UUID;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.trip.CreateTripRequest;
import com.tripnext.demo.dto.trip.TripResponse;
import com.tripnext.demo.dto.trip.UpdateTripRequest;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.TripMemberRole;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;
import com.tripnext.demo.repo.UserRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TripService {

    private final TripRepo tripRepo;
    private final UserRepo userRepo;
    private final TripMemberRepo tripMemberRepo;

    public List<TripResponse> getTrips() {

        User currentUser = getCurrentUser();

        List<TripMember> memberships =
                tripMemberRepo.findByUser(currentUser);

        return memberships.stream()
                .map(TripMember::getTrip)
                .map(trip -> new TripResponse(
                        trip.getId(),
                        trip.getTitle(),
                        trip.getDescription(),
                        trip.getDestination(),
                        trip.getStartDate(),
                        trip.getEndDate(),
                        trip.getCreatedBy().getName()
                ))
                .toList();
    }

    @Transactional
    public TripResponse createTrip(
            CreateTripRequest request
    ) {

        User currentUser = getCurrentUser();

        Trip trip = new Trip(
                request.title(),
                request.description(),
                request.destination(),
                request.startDate(),
                request.endDate(),
                currentUser
        );

        Trip savedTrip = tripRepo.save(trip);

        TripMember owner = new TripMember(
                savedTrip,
                currentUser,
                TripMemberRole.OWNER
        );

        tripMemberRepo.save(owner);

        return new TripResponse(
                savedTrip.getId(),
                savedTrip.getTitle(),
                savedTrip.getDescription(),
                savedTrip.getDestination(),
                savedTrip.getStartDate(),
                savedTrip.getEndDate(),
                currentUser.getName()
        );
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    public TripResponse getTrip(
        UUID tripId
) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isMember =
            tripMemberRepo.existsByTripAndUser(
                    trip,
                    currentUser
            );

    if (!isMember) {
        throw new UnauthorizedException(
                "You are not a member of this trip"
        );
    }

    return new TripResponse(
            trip.getId(),
            trip.getTitle(),
            trip.getDescription(),
            trip.getDestination(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getCreatedBy().getName()
    );
}
@Transactional
public TripResponse updateTrip(
        UUID tripId,
        UpdateTripRequest request
) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    trip,
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can update trip"
        );
    }

    if (request.title() != null) {
        trip.setTitle(request.title());
    }

    if (request.description() != null) {
        trip.setDescription(request.description());
    }

    if (request.destination() != null) {
        trip.setDestination(request.destination());
    }

    if (request.startDate() != null) {
        trip.setStartDate(request.startDate());
    }

    if (request.endDate() != null) {
        trip.setEndDate(request.endDate());
    }

    Trip updatedTrip = tripRepo.save(trip);

    return new TripResponse(
            updatedTrip.getId(),
            updatedTrip.getTitle(),
            updatedTrip.getDescription(),
            updatedTrip.getDestination(),
            updatedTrip.getStartDate(),
            updatedTrip.getEndDate(),
            updatedTrip.getCreatedBy().getName()
    );
}
@Transactional
public void deleteTrip(UUID tripId) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    trip,
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can delete trips"
        );
    }

    tripMemberRepo.deleteByTrip(trip);

    tripRepo.delete(trip);
}
}