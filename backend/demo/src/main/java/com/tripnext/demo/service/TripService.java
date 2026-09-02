package com.tripnext.demo.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.invitation.CreateInvitationRequest;
import com.tripnext.demo.dto.invitation.InvitationResponse;
import com.tripnext.demo.dto.invitation.InvitationSummaryResponse;
import com.tripnext.demo.dto.trip.CreateTripRequest;
import com.tripnext.demo.dto.trip.TripMemberResponse;
import com.tripnext.demo.dto.trip.TripResponse;
import com.tripnext.demo.dto.trip.UpdateMemberRoleRequest;
import com.tripnext.demo.dto.trip.UpdateTripRequest;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripInvitation;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.TripMemberRole;
import com.tripnext.demo.repo.TripInvitationRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;
import com.tripnext.demo.repo.UserRepo;
import com.tripnext.demo.util.TokenHashUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TripService {

    private final TripRepo tripRepo;
    private final UserRepo userRepo;
    private final TripMemberRepo tripMemberRepo;
    
    private final TripInvitationRepo tripInvitationRepo;

    public List<TripResponse> getTrips() {

        User currentUser = getCurrentUser();

        List<TripMember> memberships =
                tripMemberRepo.findByUser(currentUser);

        return memberships.stream()
                .map(TripMember::getTrip)
                .map(trip -> new TripResponse(
                        trip.getId(),
                        trip.getTitle(),
                        trip.getDescription(),
                        trip.getDestination(),
                        trip.getStartDate(),
                        trip.getEndDate(),
                        trip.getCreatedBy().getName()
                ))
                .toList();
    }

    @Transactional
    public TripResponse createTrip(
            CreateTripRequest request
    ) {

        User currentUser = getCurrentUser();

        Trip trip = new Trip(
                request.title(),
                request.description(),
                request.destination(),
                request.startDate(),
                request.endDate(),
                currentUser
        );
        if (request.endDate().isBefore(request.startDate())) {
    throw new IllegalArgumentException(
            "End date cannot be before start date"
    );
}

        Trip savedTrip = tripRepo.save(trip);

        TripMember owner = new TripMember(
                savedTrip,
                currentUser,
                TripMemberRole.OWNER
        );

        tripMemberRepo.save(owner);

        return new TripResponse(
                savedTrip.getId(),
                savedTrip.getTitle(),
                savedTrip.getDescription(),
                savedTrip.getDestination(),
                savedTrip.getStartDate(),
                savedTrip.getEndDate(),
                currentUser.getName()
        );
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    public TripResponse getTrip(
        UUID tripId
) {

    User currentUser = getCurrentUser();

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

    return new TripResponse(
            trip.getId(),
            trip.getTitle(),
            trip.getDescription(),
            trip.getDestination(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getCreatedBy().getName()
    );
}
@Transactional
public TripResponse updateTrip(
        UUID tripId,
        UpdateTripRequest request
) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    trip,
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can update trip"
        );
    }

    if (request.title() != null) {
        trip.setTitle(request.title());
    }

    if (request.description() != null) {
        trip.setDescription(request.description());
    }

    if (request.destination() != null) {
        trip.setDestination(request.destination());
    }

    if (request.startDate() != null) {
        trip.setStartDate(request.startDate());
    }

    if (request.endDate() != null) {
        trip.setEndDate(request.endDate());
    }
    if (request.endDate().isBefore(request.startDate())) {
    throw new IllegalArgumentException(
            "End date cannot be before start date"
    );
}

    Trip updatedTrip = tripRepo.save(trip);

    return new TripResponse(
            updatedTrip.getId(),
            updatedTrip.getTitle(),
            updatedTrip.getDescription(),
            updatedTrip.getDestination(),
            updatedTrip.getStartDate(),
            updatedTrip.getEndDate(),
            updatedTrip.getCreatedBy().getName()
    );
}
@Transactional
public void deleteTrip(UUID tripId) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    trip,
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can delete trips"
        );
    }

    tripMemberRepo.deleteByTrip(trip);

    tripRepo.delete(trip);
}

public List<TripMemberResponse> getTripMembers(
        UUID tripId
) {

    User currentUser = getCurrentUser();

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

    return tripMemberRepo.findByTrip(trip)
            .stream()
            .map(member -> new TripMemberResponse(
                    member.getUser().getId(),
                    member.getUser().getName(),
                    member.getUser().getEmail(),
                    member.getRole()
            ))
            .toList();
}
        @Transactional
public InvitationResponse createInvitation(
        UUID tripId,
        CreateInvitationRequest request
) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    trip,
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can create invitations"
        );
    }

    String rawToken =
            UUID.randomUUID().toString();

    String tokenHash =
         TokenHashUtil.hash(rawToken);

    OffsetDateTime expiresAt =
            OffsetDateTime.now().plusDays(7);

    TripInvitation invitation =
            new TripInvitation(
                    trip,
                    currentUser,
                    tokenHash,
                    request.role(),
                    expiresAt,
                    request.usageLimit()
            );

    TripInvitation saved =
            tripInvitationRepo.save(invitation);

    String inviteLink =
            "http://localhost:8080/api/invitations/accept?token="
                    + rawToken;

    return new InvitationResponse(
            saved.getId(),
            inviteLink,
            expiresAt
    );
}

        @Transactional
public void acceptInvitation(String token) {
    
    User currentUser = getCurrentUser();

    String tokenHash = TokenHashUtil.hash(token);

    TripInvitation invitation =
            tripInvitationRepo.findByTokenHash(tokenHash)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Invitation not found"
                            )
                    );
  

    if (Boolean.TRUE.equals(invitation.getIsRevoked())) {
        throw new UnauthorizedException(
                "Invitation revoked"
        );
    }

    if (invitation.getExpiresAt()
            .isBefore(OffsetDateTime.now())) {

        throw new UnauthorizedException(
                "Invitation expired"
        );
    }

    if (invitation.getUsageCount()
            >= invitation.getUsageLimit()) {

        throw new UnauthorizedException(
                "Invitation usage limit reached"
        );
    }

    Trip trip = invitation.getTrip();

    boolean alreadyMember =
            tripMemberRepo.existsByTripAndUser(
                    trip,
                    currentUser
            );

    if (alreadyMember) {
        throw new UnauthorizedException(
                "Already a member of this trip"
        );
    }

    TripMember member = new TripMember(
            trip,
            currentUser,
            TripMemberRole.valueOf(
                    invitation.getRole().name()
            )
    );

    tripMemberRepo.save(member);

    invitation.setUsageCount(
            invitation.getUsageCount() + 1
    );

    tripInvitationRepo.save(invitation);
}
     @Transactional
public void removeMember(
        UUID tripId,
        UUID userId
) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    trip,
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can remove members"
        );
    }

    User targetUser = userRepo.findById(userId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "User not found"
                    )
            );

    if (targetUser.getId().equals(currentUser.getId())) {
        throw new UnauthorizedException(
                "Owner cannot remove himself"
        );
    }

    TripMember membership =
            tripMemberRepo.findByTripAndUser(
                    trip,
                    targetUser
            )
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Member not found in trip"
                    )
            );

    tripMemberRepo.delete(membership);
} 
        @Transactional
public void updateMemberRole(
        UUID tripId,
        UUID userId,
        UpdateMemberRoleRequest request
) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    trip,
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can update roles"
        );
    }

    User targetUser = userRepo.findById(userId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "User not found"
                    )
            );

    if (targetUser.getId().equals(currentUser.getId())) {
        throw new UnauthorizedException(
                "Owner cannot modify his own role"
        );
    }

    TripMember membership =
            tripMemberRepo.findByTripAndUser(
                    trip,
                    targetUser
            )
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Member not found"
                    )
            );

    if (request.role() == TripMemberRole.OWNER) {
        throw new UnauthorizedException(
                "OWNER role cannot be assigned"
        );
    }

    membership.setRole(request.role());

    tripMemberRepo.save(membership);
}
     @Transactional
public void revokeInvitation(
        UUID invitationId
) {

    User currentUser = getCurrentUser();

    TripInvitation invitation =
            tripInvitationRepo.findById(
                    invitationId
            )
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Invitation not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    invitation.getTrip(),
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can revoke invitations"
        );
    }

    invitation.setIsRevoked(true);

    tripInvitationRepo.save(invitation);
} 
        public List<InvitationSummaryResponse> getInvitations(
        UUID tripId
) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isOwner =
            tripMemberRepo.existsByTripAndUserAndRole(
                    trip,
                    currentUser,
                    TripMemberRole.OWNER
            );

    if (!isOwner) {
        throw new UnauthorizedException(
                "Only owners can view invitations"
        );
    }

    return tripInvitationRepo.findByTrip(trip)
            .stream()
            .map(invitation -> new InvitationSummaryResponse(
                    invitation.getId(),
                    invitation.getRole(),
                    invitation.getUsageLimit(),
                    invitation.getUsageCount(),
                    invitation.getIsRevoked(),
                    invitation.getExpiresAt()
            ))
            .toList();
}
        @Transactional
public void leaveTrip(UUID tripId) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    TripMember membership =
            tripMemberRepo.findByTripAndUser(
                    trip,
                    currentUser
            )
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "You are not a member of this trip"
                    )
            );

    if (membership.getRole() == TripMemberRole.OWNER) {
        throw new UnauthorizedException(
                "Owner cannot leave trip"
        );
    }

    tripMemberRepo.delete(membership);
}
     @Transactional
public void transferOwnership(
        UUID tripId,
        UUID newOwnerUserId
) {

    User currentUser = getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    TripMember currentOwner =
            tripMemberRepo.findByTripAndUser(
                    trip,
                    currentUser
            )
            .orElseThrow(() ->
                    new UnauthorizedException(
                            "Not a member"
                    )
            );
            
    if (currentOwner.getUser().getId().equals(newOwnerUserId)) {
    throw new IllegalArgumentException(
            "Cannot transfer ownership to yourself"
    );
}
    if (currentOwner.getRole() != TripMemberRole.OWNER) {
        throw new UnauthorizedException(
                "Only owner can transfer ownership"
        );
    }
    
    User newOwnerUser = userRepo.findById(
            newOwnerUserId
    ).orElseThrow(() ->
            new ResourceNotFoundException(
                    "User not found"
            )
    );
    

    TripMember newOwnerMembership =
            tripMemberRepo.findByTripAndUser(
                    trip,
                    newOwnerUser
            )
            .orElseThrow(() ->
                    new UnauthorizedException(
                            "User is not a trip member"
                    )
            );

    currentOwner.setRole(
            TripMemberRole.ADMIN
    );

    newOwnerMembership.setRole(
            TripMemberRole.OWNER
    );

    tripMemberRepo.save(currentOwner);
    tripMemberRepo.save(newOwnerMembership);
}
}