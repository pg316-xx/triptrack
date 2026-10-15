package com.tripnext.demo.dto.itinerary;

import java.util.UUID;

/**
 * Drag-and-drop move.
 *
 * @param targetDayId     day to move into; null keeps the current day
 * @param afterActivityId activity to place it after; null puts it first
 * @param version         the activity's current version (optimistic lock)
 */
public record MoveActivityRequest(
        UUID targetDayId,
        UUID afterActivityId,
        Integer version
) {}
