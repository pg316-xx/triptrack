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
import com.tripnext.demo.dto.itinerary.ItineraryEvent;
import com.tripnext.demo.dto.itinerary.ItineraryResponse;
import com.tripnext.demo.dto.itinerary.MoveActivityRequest;
import com.tripnext.demo.dto.itinerary.UpdateActivityRequest;
import com.tripnext.demo.dto.itinerary.UpdateItineraryDayRequest;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.ConflictException;
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
import com.tripnext.demo.util.LexoRank;


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
    private final ItineraryEventPublisher itineraryEventPublisher;

    private void publish(
            Trip trip,
            String type,
            UUID dayId,
            UUID activityId,
            User actor,
            Object data
    ) {
        itineraryEventPublisher.publish(
                trip.getId(),
                new ItineraryEvent(type, dayId, activityId, actor.getId(), data)
        );
    }

    private ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getLocation(),
                activity.getLatitude(),
                activity.getLongitude(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getEstimatedCost(),
                activity.getOrderIndex(),
                activity.getVersion()
        );
    }

    private void validateActivityFields(
            String title,
            java.time.OffsetDateTime startTime,
            java.time.OffsetDateTime endTime,
            BigDecimal estimatedCost
    ) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException(
                    "Title is required"
            );
        }
        if (startTime != null && endTime != null
                && !endTime.isAfter(startTime)) {
            throw new BadRequestException(
                    "End time must be after start time"
            );
        }
        if (estimatedCost != null
                && estimatedCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "Estimated cost cannot be negative"
            );
        }
    }

    private void checkVersion(
            Activity activity,
            Integer version
    ) {
        if (version == null) {
            throw new BadRequestException(
                    "version is required"
            );
        }
        if (!activity.getVersion().equals(version)) {
            throw new ConflictException(
                    "Activity was modified by another user. Reload and try again."
            );
        }
    }

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
@Transactional
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
    throw new BadRequestException(
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
    throw new BadRequestException(
            "Day number must be greater than 0"
    );
}
if (request.date().isBefore(trip.getStartDate())
        || request.date().isAfter(trip.getEndDate())) {

    throw new BadRequestException(
            "Date must be within trip duration"
    );
}

    ItineraryDay saved =
            itineraryDayRepo.save(day);

    ItineraryDayResponse response = new ItineraryDayResponse(
            saved.getId(),
            saved.getDayNumber(),
            saved.getDate(),
            saved.getTitle(),
            saved.getNotes()
    );

    publish(trip, "DAY_CREATED", saved.getId(), null, currentUser, response);

    return response;
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

    ItineraryDayResponse response = new ItineraryDayResponse(
            saved.getId(),
            saved.getDayNumber(),
            saved.getDate(),
            saved.getTitle(),
            saved.getNotes()
    );

    publish(trip, "DAY_UPDATED", saved.getId(), null, currentUser, response);

    return response;
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

    publish(trip, "DAY_DELETED", dayId, null, currentUser, null);
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

    validateActivityFields(
            request.title(),
            request.startTime(),
            request.endTime(),
            request.estimatedCost()
    );

    // Append to the end of the day.
    String rank = LexoRank.between(
            activityRepo.findFirstByItineraryDayOrderByOrderIndexDesc(day)
                    .map(Activity::getOrderIndex)
                    .orElse(null),
            null
    );

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
            rank
    );

    Activity saved =
            activityRepo.saveAndFlush(activity);

    ActivityResponse response = toResponse(saved);

    publish(trip, "ACTIVITY_CREATED", day.getId(), saved.getId(), currentUser, response);

    return response;
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

    checkVersion(activity, request.version());

    validateActivityFields(
            request.title(),
            request.startTime(),
            request.endTime(),
            request.estimatedCost()
    );

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

    // Flush so the @Version bump (and any concurrent-write conflict) happens
    // here, and the response/broadcast carry the new version.
    Activity saved =
            activityRepo.saveAndFlush(activity);

    ActivityResponse response = toResponse(saved);

    publish(trip, "ACTIVITY_UPDATED", saved.getItineraryDay().getId(), saved.getId(), currentUser, response);

    return response;
}
@Transactional
public ActivityResponse moveActivity(
        UUID activityId,
        MoveActivityRequest request
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

    checkVersion(activity, request.version());

    ItineraryDay targetDay = activity.getItineraryDay();
    if (request.targetDayId() != null
            && !request.targetDayId().equals(targetDay.getId())) {
        targetDay = itineraryDayRepo.findById(request.targetDayId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Itinerary day not found"
                        )
                );
        if (!targetDay.getTrip().getId().equals(trip.getId())) {
            throw new BadRequestException(
                    "Target day belongs to another trip"
            );
        }
    }

    // Find the two neighbours in the target day and take a rank between them.
    String before;
    String after;
    if (request.afterActivityId() == null) {
        before = null;
        after = activityRepo.findFirstByItineraryDayOrderByOrderIndexAsc(targetDay)
                .filter(first -> !first.getId().equals(activity.getId()))
                .map(Activity::getOrderIndex)
                .orElse(null);
    } else {
        if (request.afterActivityId().equals(activity.getId())) {
            throw new BadRequestException(
                    "An activity cannot be placed after itself"
            );
        }
        Activity anchor = activityRepo.findById(request.afterActivityId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Anchor activity not found"
                        )
                );
        if (!anchor.getItineraryDay().getId().equals(targetDay.getId())) {
            throw new BadRequestException(
                    "Anchor activity is not in the target day"
            );
        }
        before = anchor.getOrderIndex();
        after = activityRepo
                .findFirstByItineraryDayAndOrderIndexGreaterThanAndIdNotOrderByOrderIndexAsc(
                        targetDay, before, activity.getId()
                )
                .map(Activity::getOrderIndex)
                .orElse(null);
    }

    // Two activities created at the same moment can share a rank; if the
    // neighbours collide, just place it after the anchor's group.
    if (before != null && after != null && before.compareTo(after) >= 0) {
        after = null;
    }

    activity.setItineraryDay(targetDay);
    activity.setOrderIndex(LexoRank.between(before, after));

    Activity saved =
            activityRepo.saveAndFlush(activity);

    ActivityResponse response = toResponse(saved);

    publish(trip, "ACTIVITY_MOVED", targetDay.getId(), saved.getId(), currentUser, response);

    return response;
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

    UUID dayId = activity.getItineraryDay().getId();

    activityRepo.delete(activity);

    publish(trip, "ACTIVITY_DELETED", dayId, activityId, currentUser, null);
}
@Transactional
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
                                                        activity.getOrderIndex(),
                                                        activity.getVersion()
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
