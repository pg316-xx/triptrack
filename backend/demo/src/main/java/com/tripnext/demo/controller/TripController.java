package com.tripnext.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.trip.CreateTripRequest;
import com.tripnext.demo.dto.trip.TripResponse;

import com.tripnext.demo.service.TripService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<List<TripResponse>> getAllTrips() {

        return ResponseEntity.ok(
                tripService.getTrips()
        );
    }

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @Valid @RequestBody CreateTripRequest newTrip
    ) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        tripService.createTrip(newTrip)
                );
    }
}