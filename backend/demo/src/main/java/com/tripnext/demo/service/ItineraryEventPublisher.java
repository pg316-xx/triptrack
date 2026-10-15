package com.tripnext.demo.service;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tripnext.demo.dto.itinerary.ItineraryEvent;

import lombok.RequiredArgsConstructor;

/**
 * Broadcasts itinerary changes to /topic/trip/{tripId}/itinerary.
 * Frames are sent only after the surrounding transaction commits, so clients
 * never see a change that was rolled back (e.g. a 409 version conflict).
 */
@Component
@RequiredArgsConstructor
public class ItineraryEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(
            UUID tripId,
            ItineraryEvent event
    ) {

        String destination =
                "/topic/trip/" + tripId + "/itinerary";

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            messagingTemplate.convertAndSend(destination, event);
                        }
                    }
            );
        } else {
            messagingTemplate.convertAndSend(destination, event);
        }
    }
}
