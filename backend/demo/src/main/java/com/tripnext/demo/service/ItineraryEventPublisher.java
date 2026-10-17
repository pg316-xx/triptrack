package com.tripnext.demo.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tripnext.demo.dto.itinerary.ItineraryEvent;

import lombok.RequiredArgsConstructor;

/** Broadcasts itinerary changes to /topic/trip/{tripId}/itinerary (after commit, on every instance). */
@Component
@RequiredArgsConstructor
public class ItineraryEventPublisher {

    private final RealtimeBroadcaster realtimeBroadcaster;

    public void publish(
            UUID tripId,
            ItineraryEvent event
    ) {
        realtimeBroadcaster.send("/topic/trip/" + tripId + "/itinerary", event);
    }
}
