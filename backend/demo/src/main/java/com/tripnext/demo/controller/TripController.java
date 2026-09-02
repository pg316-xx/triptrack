package com.tripnext.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.invitation.CreateInvitationRequest;
import com.tripnext.demo.dto.invitation.InvitationResponse;
import com.tripnext.demo.dto.invitation.InvitationSummaryResponse;
import com.tripnext.demo.dto.trip.CreateTripRequest;
import com.tripnext.demo.dto.trip.TransferOwnershipRequest;
import com.tripnext.demo.dto.trip.TripMemberResponse;
import com.tripnext.demo.dto.trip.TripResponse;
import com.tripnext.demo.dto.trip.UpdateMemberRoleRequest;
import com.tripnext.demo.dto.trip.UpdateTripRequest;
import com.tripnext.demo.service.TripService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
    @GetMapping("/{tripId}")
     public ResponseEntity<TripResponse> getTrip(
        @PathVariable UUID tripId
    ) {
    return ResponseEntity.ok(
            tripService.getTrip(tripId)
    );
   }
   @PatchMapping("/{tripId}")
public ResponseEntity<TripResponse> updateTrip(
        @PathVariable UUID tripId,
        @RequestBody UpdateTripRequest request
) {

    return ResponseEntity.ok(
            tripService.updateTrip(
                    tripId,
                    request
            )
    );
}
     @DeleteMapping("/{tripId}")
     public ResponseEntity<Void> deleteTrip(
        @PathVariable UUID tripId
     ) {

    tripService.deleteTrip(tripId);

    return ResponseEntity.noContent().build();
}
    @GetMapping("/{tripId}/members")
   public ResponseEntity<List<TripMemberResponse>>
    getTripMembers(
        @PathVariable UUID tripId
    ) {

    return ResponseEntity.ok(
            tripService.getTripMembers(tripId)
    );
}  
      @PostMapping("/{tripId}/invitations")
public ResponseEntity<InvitationResponse>
createInvitation(
        @PathVariable UUID tripId,
        @RequestBody CreateInvitationRequest request
) {

    return ResponseEntity.ok(
            tripService.createInvitation(
                    tripId,
                    request
            )
    );
} 
@DeleteMapping("/{tripId}/members/{userId}")
public ResponseEntity<Void> removeMember(
        @PathVariable UUID tripId,
        @PathVariable UUID userId
) {

    tripService.removeMember(
            tripId,
            userId
    );

    return ResponseEntity.noContent().build();
}     
      @PatchMapping("/{tripId}/members/{userId}/role")
public ResponseEntity<Void> updateMemberRole(
        @PathVariable UUID tripId,
        @PathVariable UUID userId,
        @RequestBody UpdateMemberRoleRequest request
) {

    tripService.updateMemberRole(
            tripId,
            userId,
            request
    );

    return ResponseEntity.ok().build();
}
      @GetMapping("/{tripId}/invitations")
public ResponseEntity<List<InvitationSummaryResponse>>
getInvitations(
        @PathVariable UUID tripId
) {

    return ResponseEntity.ok(
            tripService.getInvitations(tripId)
    );
}
       @PostMapping("/{tripId}/leave")
public ResponseEntity<Void> leaveTrip(
        @PathVariable UUID tripId
) {

    tripService.leaveTrip(tripId);

    return ResponseEntity.ok().build();
}
      @PatchMapping("/{tripId}/owner")
public ResponseEntity<Void> transferOwnership(
        @PathVariable UUID tripId,
        @RequestBody TransferOwnershipRequest request
) {

    tripService.transferOwnership(
            tripId,
            request.userId()
    );

    return ResponseEntity.ok().build();
}

}