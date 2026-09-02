package com.tripnext.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.itinerary.ActivityResponse;
import com.tripnext.demo.dto.itinerary.CreateActivityRequest;
import com.tripnext.demo.dto.itinerary.CreateItineraryDayRequest;
import com.tripnext.demo.dto.itinerary.ItineraryDayResponse;
import com.tripnext.demo.dto.itinerary.ItineraryResponse;
import com.tripnext.demo.dto.itinerary.UpdateActivityRequest;
import com.tripnext.demo.dto.itinerary.UpdateItineraryDayRequest;
import com.tripnext.demo.service.ItineraryDayService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/trips/{tripId}/days")
@RequiredArgsConstructor
public class ItineraryDayController {
    private final ItineraryDayService itineraryDayService;
   @GetMapping
public ResponseEntity<List<ItineraryDayResponse>> getDays(
        @PathVariable UUID tripId
) {

    return ResponseEntity.ok(
            itineraryDayService.getItineraryDays(
                    tripId
            )
    );
}

@PostMapping
public ResponseEntity<ItineraryDayResponse> createDay(
        @PathVariable UUID tripId,
        @RequestBody CreateItineraryDayRequest request
) {

    return ResponseEntity.ok(
            itineraryDayService.createItineraryDay(
                    tripId,
                    request
            )
    );
}
@PatchMapping("/{dayId}")
public ResponseEntity<ItineraryDayResponse>
updateItineraryDay(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId,
        @RequestBody UpdateItineraryDayRequest request
) {

    return ResponseEntity.ok(
            itineraryDayService.updateItineraryDay(
                    tripId,
                    dayId,
                    request
            )
    );
}
@DeleteMapping("/{dayId}")
public ResponseEntity<Void> deleteItineraryDay(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId
) {

    itineraryDayService.deleteItineraryDay(
            tripId,
            dayId
    );

    return ResponseEntity.noContent().build();
}
@PostMapping("/{dayId}/activities")
public ResponseEntity<ActivityResponse> createActivity(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId,
        @RequestBody CreateActivityRequest request
) {

    ActivityResponse response =
            itineraryDayService.createActivity(
                    request,
                    tripId,
                    dayId
            );

    return ResponseEntity.status(201)
            .body(response);
}
@PatchMapping("/api/activities/{activityId}")
public ResponseEntity<ActivityResponse> updateActivity(
        @PathVariable UUID activityId,
        @RequestBody UpdateActivityRequest request
) {
    return ResponseEntity.ok(
            itineraryDayService.updateActivity(
                    activityId,
                    request
            )
    );
}
@DeleteMapping("/api/activities/{activityId}")
public ResponseEntity<Void> deleteActivity(
        @PathVariable UUID activityId
) {

    itineraryDayService.deleteActivity(
            activityId
    );

    return ResponseEntity.noContent()
            .build();
}
@GetMapping("/itinerary")
public ResponseEntity<ItineraryResponse> getItinerary(
        @PathVariable UUID tripId
) {

    return ResponseEntity.ok(
            itineraryDayService.getItinerary(
                    tripId
            )
    );
}
    
}
