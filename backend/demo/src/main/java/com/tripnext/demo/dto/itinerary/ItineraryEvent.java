package com.tripnext.demo.dto.itinerary;

import java.util.UUID;

/**
 * Frame sent to /topic/trip/{tripId}/itinerary after an itinerary change commits.
 *
 * @param type       DAY_CREATED, DAY_UPDATED, DAY_DELETED, ACTIVITY_CREATED,
 *                   ACTIVITY_UPDATED, ACTIVITY_MOVED or ACTIVITY_DELETED
 * @param dayId      affected day (the destination day for moves)
 * @param activityId affected activity, null for day events
 * @param actorId    user who made the change
 * @param data       the day/activity after the change, null for deletes
 */
public record ItineraryEvent(
        String type,
        UUID dayId,
        UUID activityId,
        UUID actorId,
        Object data
) {}
