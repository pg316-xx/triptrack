package com.tripnext.demo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.itinerary.ActivityResponse;
import com.tripnext.demo.dto.itinerary.ActivitySummaryResponse;
import com.tripnext.demo.dto.itinerary.CreateActivityRequest;
import com.tripnext.demo.dto.itinerary.CreateItineraryDayRequest;
import com.tripnext.demo.dto.itinerary.ItineraryDayDetailResponse;
import com.tripnext.demo.dto.itinerary.ItineraryDayResponse;
import com.tripnext.demo.dto.itinerary.ItineraryResponse;
import com.tripnext.demo.dto.itinerary.UpdateActivityRequest;
import com.tripnext.demo.dto.itinerary.UpdateItineraryDayRequest;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.Activity;
import com.tripnext.demo.model.ItineraryDay;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.TripMemberRole;
import com.tripnext.demo.repo.ActivityRepo;
import com.tripnext.demo.repo.ItineraryDayRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class ItineraryDayService {
    private final ItineraryDayRepo itineraryDayRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final ActivityRepo activityRepo;
    private final CurrentUserService currentUserService;
    private void validateTripAdminAccess(
            Trip trip,
            User user
    ) {

        TripMember member =
                tripMemberRepo.findByTripAndUser(
                        trip,
                        user
                ).orElseThrow(() ->
                        new UnauthorizedException(
                                "Not a member of this trip"
                        )
                );

        if (member.getRole() != TripMemberRole.OWNER
                && member.getRole() != TripMemberRole.ADMIN) {

            throw new UnauthorizedException(
                    "Insufficient permissions"
            );
        }
    }
    public List<ItineraryDayResponse> getItineraryDays(
        UUID tripId
) {

    User currentUser =
        currentUserService.getCurrentUser();

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

    return itineraryDayRepo
            .findByTripOrderByDayNumber(trip)
            .stream()
            .map(day -> new ItineraryDayResponse(
                    day.getId(),
                    day.getDayNumber(),
                    day.getDate(),
                    day.getTitle(),
                    day.getNotes()
            ))
            .toList();
}

    @Transactional
public ItineraryDayResponse createItineraryDay(
        UUID tripId,
        CreateItineraryDayRequest request
) {

    User currentUser =
        currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    validateTripAdminAccess(
        trip,
        currentUser
);
    if (itineraryDayRepo.existsByTripAndDayNumber(
        trip,
        request.dayNumber()
)) {
    throw new IllegalArgumentException(
            "Day number already exists"
    );
}
    ItineraryDay day = new ItineraryDay(
            trip,
            request.dayNumber(),
            request.date(),
            request.title(),
            request.notes()
    );
    if (request.dayNumber() <= 0) {
    throw new IllegalArgumentException(
            "Day number must be greater than 0"
    );
}
if (request.date().isBefore(trip.getStartDate())
        || request.date().isAfter(trip.getEndDate())) {

    throw new IllegalArgumentException(
            "Date must be within trip duration"
    );
}

    ItineraryDay saved =
            itineraryDayRepo.save(day);

    return new ItineraryDayResponse(
            saved.getId(),
            saved.getDayNumber(),
            saved.getDate(),
            saved.getTitle(),
            saved.getNotes()
    );
}
    @Transactional
public ItineraryDayResponse updateItineraryDay(
        UUID tripId,
        UUID dayId,
        UpdateItineraryDayRequest request
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    validateTripAdminAccess(
        trip,
        currentUser
);

    ItineraryDay day =
            itineraryDayRepo.findById(dayId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Itinerary day not found"
                            )
                    );

    if (!day.getTrip().getId().equals(tripId)) {
        throw new UnauthorizedException(
                "Day does not belong to this trip"
        );
    }

    day.setTitle(request.title());
    day.setNotes(request.notes());
     
    ItineraryDay saved =
            itineraryDayRepo.save(day);

    return new ItineraryDayResponse(
            saved.getId(),
            saved.getDayNumber(),
            saved.getDate(),
            saved.getTitle(),
            saved.getNotes()
    );
}
@Transactional
public void deleteItineraryDay(
        UUID tripId,
        UUID dayId
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    validateTripAdminAccess(
        trip,
        currentUser
);

    ItineraryDay day =
            itineraryDayRepo.findById(dayId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Itinerary day not found"
                            )
                    );

    if (!day.getTrip().getId().equals(tripId)) {
        throw new UnauthorizedException(
                "Day does not belong to this trip"
        );
    }

    itineraryDayRepo.delete(day);
}
@Transactional
public ActivityResponse createActivity(
        CreateActivityRequest request,
        UUID tripId,
        UUID dayId
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    validateTripAdminAccess(
            trip,
            currentUser
    );
    
    ItineraryDay day =
            itineraryDayRepo.findById(dayId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Itinerary day not found"
                            )
                    );

    if (!day.getTrip().getId().equals(tripId)) {
        throw new UnauthorizedException(
                "Day does not belong to this trip"
        );
    }
    if (activityRepo.existsByItineraryDayAndOrderIndex(
        day,
        request.orderIndex()
)) {
    throw new IllegalArgumentException(
            "Order index already exists"
    );
}

    Activity activity = new Activity(
            day,
            currentUser,
            request.title(),
            request.description(),
            request.location(),
            request.latitude(),
            request.longitude(),
            request.startTime(),
            request.endTime(),
            request.estimatedCost(),
            request.orderIndex()
    );
    if (!request.endTime()
        .isAfter(request.startTime())) {

    throw new IllegalArgumentException(
            "End time must be after start time"
    );
}
    if (request.estimatedCost()
        .compareTo(BigDecimal.ZERO) < 0) {

    throw new IllegalArgumentException(
            "Estimated cost cannot be negative"
    );
}

    Activity saved =
            activityRepo.save(activity);

    return new ActivityResponse(
            saved.getId(),
            saved.getTitle(),
            saved.getDescription(),
            saved.getLocation(),
            saved.getLatitude(),
            saved.getLongitude(),
            saved.getStartTime(),
            saved.getEndTime(),
            saved.getEstimatedCost(),
            saved.getOrderIndex(),
            saved.getVersion()
    );
}
@Transactional
public ActivityResponse updateActivity(
        UUID activityId,
        UpdateActivityRequest request
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Activity activity =
            activityRepo.findById(activityId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Activity not found"
                            )
                    );

    Trip trip = activity
            .getItineraryDay()
            .getTrip();

    validateTripAdminAccess(
            trip,
            currentUser
    );

    if (!activity.getVersion()
            .equals(request.version())) {

        throw new IllegalStateException(
                "Activity was modified by another user"
        );
    }

    activity.setTitle(
            request.title()
    );
    activity.setDescription(
            request.description()
    );
    activity.setLocation(
            request.location()
    );
    activity.setLatitude(
            request.latitude()
    );
    activity.setLongitude(
            request.longitude()
    );
    activity.setStartTime(
            request.startTime()
    );
    activity.setEndTime(
            request.endTime()
    );
    activity.setEstimatedCost(
            request.estimatedCost()
    );
    activity.setOrderIndex(
            request.orderIndex()
    );
    

    Activity saved =
            activityRepo.save(activity);

    return new ActivityResponse(
            saved.getId(),
            saved.getTitle(),
            saved.getDescription(),
            saved.getLocation(),
            saved.getLatitude(),
            saved.getLongitude(),
            saved.getStartTime(),
            saved.getEndTime(),
            saved.getEstimatedCost(),
            saved.getOrderIndex(),
            saved.getVersion()
    );
}
@Transactional
public void deleteActivity(
        UUID activityId
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Activity activity =
            activityRepo.findById(activityId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Activity not found"
                            )
                    );

    Trip trip = activity
            .getItineraryDay()
            .getTrip();

    validateTripAdminAccess(
            trip,
            currentUser
    );

    activityRepo.delete(activity);
}
public ItineraryResponse getItinerary(
        UUID tripId
) {

    User currentUser =
            currentUserService.getCurrentUser();

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

    List<ItineraryDayDetailResponse> days =
            itineraryDayRepo
                    .findByTripOrderByDayNumber(trip)
                    .stream()
                    .map(day -> {

                        List<ActivitySummaryResponse> activities =
                                activityRepo
                                        .findByItineraryDayOrderByOrderIndex(day)
                                        .stream()
                                        .map(activity ->
                                                new ActivitySummaryResponse(
                                                        activity.getId(),
                                                        activity.getTitle(),
                                                        activity.getDescription(),
                                                        activity.getLocation(),
                                                        activity.getLatitude(),
                                                        activity.getLongitude(),
                                                        activity.getStartTime(),
                                                        activity.getEndTime(),
                                                        activity.getEstimatedCost(),
                                                        activity.getOrderIndex()
                                                )
                                        )
                                        .toList();

                        return new ItineraryDayDetailResponse(
                                day.getId(),
                                day.getDayNumber(),
                                day.getDate(),
                                day.getTitle(),
                                day.getNotes(),
                                activities
                        );
                    })
                    .toList();

    return new ItineraryResponse(
            tripId,
            days
    );
}
}
