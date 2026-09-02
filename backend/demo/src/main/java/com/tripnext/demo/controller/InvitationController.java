package com.tripnext.demo.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tripnext.demo.service.TripService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final TripService tripService;
    
    @PostMapping("/accept")
    public ResponseEntity<Void> acceptInvitation(
            @RequestParam String token
    ) {

        tripService.acceptInvitation(token);

        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable UUID invitationId
    ) {

        tripService.revokeInvitation(
                invitationId
        );

        return ResponseEntity.noContent().build();
    }
}