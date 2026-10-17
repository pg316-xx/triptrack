package com.tripnext.demo.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tripnext.demo.dto.invitation.CreateInvitationRequest;
import com.tripnext.demo.dto.invitation.InvitationResponse;
import com.tripnext.demo.dto.invitation.InvitationSummaryResponse;
import com.tripnext.demo.dto.trip.CreateTripRequest;
import com.tripnext.demo.dto.trip.TripMemberResponse;
import com.tripnext.demo.dto.trip.TripResponse;
import com.tripnext.demo.dto.trip.UpdateMemberRoleRequest;
import com.tripnext.demo.dto.trip.UpdateTripRequest;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripInvitation;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.NotificationType;
import com.tripnext.demo.model.help.TripMemberRole;
import com.tripnext.demo.model.help.TripRole;
import com.tripnext.demo.repo.DocumentRepo;
import com.tripnext.demo.repo.TripInvitationRepo;
import com.tripnext.demo.repo.TripMediaRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;
import com.tripnext.demo.repo.UserRepo;
import com.tripnext.demo.service.storage.FileStorageService;
import com.tripnext.demo.service.storage.StorageBucket;
import com.tripnext.demo.util.TokenHashUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Trips and membership. Roles are hierarchical: OWNER > ADMIN > MEMBER (SRS FR-TRIP-004).
 *   OWNER  delete trip, change roles, transfer ownership, remove anyone
 *   ADMIN  edit trip details, invite people, remove MEMBERs
 *   MEMBER read everything, collaborate
 */
@RequiredArgsConstructor
@Service
public class TripService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final TripRepo tripRepo;
    private final UserRepo userRepo;
    private final TripMemberRepo tripMemberRepo;
    private final TripInvitationRepo tripInvitationRepo;
    private final DocumentRepo documentRepo;
    private final TripMediaRepo tripMediaRepo;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ---------- trips ----------

    @Transactional
    public Page<TripResponse> getTrips(Pageable pageable) {
        return tripRepo.findTripsForUser(getCurrentUser(), pageable).map(this::toResponse);
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        User currentUser = getCurrentUser();

        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (request.destination() == null || request.destination().isBlank()) {
            throw new BadRequestException("Destination is required");
        }
        if (request.startDate() == null || request.endDate() == null) {
            throw new BadRequestException("Start and end dates are required");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Trip trip = new Trip(
                request.title().trim(),
                request.description(),
                request.destination().trim(),
                request.startDate(),
                request.endDate(),
                currentUser
        );
        trip.setCoverImageUrl(cleanCoverUrl(request.coverImageUrl()));

        Trip saved = tripRepo.save(trip);
        tripMemberRepo.save(new TripMember(saved, currentUser, TripMemberRole.OWNER));

        auditLogService.logAction(saved, "TRIP_CREATED", "Trip", saved.getId(),
                "Created trip: " + saved.getTitle());

        return toResponse(saved);
    }

    // Not cached: the membership check below must run for every caller.
    @Transactional
    public TripResponse getTrip(UUID tripId) {
        Trip trip = requireTrip(tripId);
        requireRole(trip, getCurrentUser(), TripMemberRole.MEMBER);
        return toResponse(trip);
    }

    @Transactional
    @CacheEvict(value = "trips", key = "#tripId")
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request) {
        User currentUser = getCurrentUser();
        Trip trip = requireTrip(tripId);
        requireRole(trip, currentUser, TripMemberRole.ADMIN);

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("Title cannot be empty");
            }
            trip.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            trip.setDescription(request.description());
        }
        if (request.destination() != null) {
            if (request.destination().isBlank()) {
                throw new BadRequestException("Destination cannot be empty");
            }
            trip.setDestination(request.destination().trim());
        }
        if (request.startDate() != null) {
            trip.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            trip.setEndDate(request.endDate());
        }
        if (request.coverImageUrl() != null) {
            trip.setCoverImageUrl(cleanCoverUrl(request.coverImageUrl()));
        }
        // Check the resulting dates, so a partial update (e.g. only the title) works.
        if (trip.getEndDate().isBefore(trip.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Trip updated = tripRepo.save(trip);

        auditLogService.logAction(updated, "UPDATE", "Trip", updated.getId(), "Trip details updated");
        notifyMembers(updated, currentUser, NotificationType.TRIP_UPDATED, "Trip updated",
                currentUser.getName() + " updated \"" + updated.getTitle() + "\"");

        return toResponse(updated);
    }

    /**
     * Deletes the trip and, via ON DELETE CASCADE (V6), its members, invites,
     * itinerary, expenses, settlements, chat, documents and media. Audit logs
     * and notifications are kept with trip_id = NULL; wallet items are kept
     * and unlinked. Stored document/media files are removed after commit.
     */
    @Transactional
    @CacheEvict(value = "trips", key = "#tripId")
    public void deleteTrip(UUID tripId) {
        Trip trip = requireTrip(tripId);
        requireRole(trip, getCurrentUser(), TripMemberRole.OWNER);

        List<String> documentKeys = documentRepo.findByTripOrderByCreatedAtDesc(trip)
                .stream().map(d -> d.getStorageKey()).toList();
        List<String> mediaKeys = tripMediaRepo.findByTripOrderByCreatedAtDesc(trip)
                .stream().map(m -> m.getStorageKey()).toList();

        auditLogService.logAction(trip, "TRIP_DELETED", "Trip", trip.getId(),
                "Deleted trip: " + trip.getTitle());

        tripRepo.delete(trip);

        afterCommit(() -> {
            documentKeys.forEach(k -> fileStorageService.delete(StorageBucket.DOCUMENTS, k));
            mediaKeys.forEach(k -> fileStorageService.delete(StorageBucket.MEDIA, k));
        });
    }

    // ---------- members ----------

    @Transactional
    public List<TripMemberResponse> getTripMembers(UUID tripId) {
        Trip trip = requireTrip(tripId);
        requireRole(trip, getCurrentUser(), TripMemberRole.MEMBER);

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
    public void removeMember(UUID tripId, UUID userId) {
        User currentUser = getCurrentUser();
        Trip trip = requireTrip(tripId);
        TripMember actor = requireRole(trip, currentUser, TripMemberRole.ADMIN);

        if (userId.equals(currentUser.getId())) {
            throw new BadRequestException("Use \"leave trip\" to remove yourself");
        }

        User targetUser = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TripMember membership = tripMemberRepo.findByTripAndUser(trip, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in trip"));

        // Admins can remove members; only the owner can remove an admin. Nobody removes the owner.
        if (membership.getRole() == TripMemberRole.OWNER
                || (membership.getRole() == TripMemberRole.ADMIN && actor.getRole() != TripMemberRole.OWNER)) {
            throw new UnauthorizedException("You can't remove this member");
        }

        tripMemberRepo.delete(membership);

        auditLogService.logAction(trip, "MEMBER_REMOVED", "TripMember", targetUser.getId(),
                "Removed user: " + targetUser.getName());
        notificationService.createNotification(targetUser, null, NotificationType.MEMBER_REMOVED,
                "Removed from a trip",
                currentUser.getName() + " removed you from \"" + trip.getTitle() + "\"",
                null);
    }

    @Transactional
    public void updateMemberRole(UUID tripId, UUID userId, UpdateMemberRoleRequest request) {
        User currentUser = getCurrentUser();
        Trip trip = requireTrip(tripId);
        requireRole(trip, currentUser, TripMemberRole.OWNER);

        if (request.role() == null) {
            throw new BadRequestException("role is required");
        }
        if (request.role() == TripMemberRole.OWNER) {
            throw new BadRequestException("Use transfer ownership to make someone the owner");
        }
        if (userId.equals(currentUser.getId())) {
            throw new BadRequestException("The owner's role can't be changed");
        }

        User targetUser = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TripMember membership = tripMemberRepo.findByTripAndUser(trip, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        membership.setRole(request.role());
        tripMemberRepo.save(membership);

        auditLogService.logAction(trip, "ROLE_CHANGED", "TripMember", targetUser.getId(),
                "Role updated to: " + request.role().name());
        notificationService.createNotification(targetUser, trip, NotificationType.ROLE_CHANGED,
                "Your role changed",
                "You are now " + article(request.role()) + " " + request.role().name().toLowerCase()
                        + " of \"" + trip.getTitle() + "\"",
                "/trips/" + trip.getId());
    }

    @Transactional
    public void leaveTrip(UUID tripId) {
        User currentUser = getCurrentUser();
        Trip trip = requireTrip(tripId);

        TripMember membership = tripMemberRepo.findByTripAndUser(trip, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this trip"));

        if (membership.getRole() == TripMemberRole.OWNER) {
            throw new BadRequestException("Transfer ownership before leaving the trip");
        }

        tripMemberRepo.delete(membership);
        notifyAdmins(trip, currentUser, NotificationType.MEMBER_REMOVED, "A member left",
                currentUser.getName() + " left \"" + trip.getTitle() + "\"");
    }

    @Transactional
    public void transferOwnership(UUID tripId, UUID newOwnerUserId) {
        User currentUser = getCurrentUser();
        Trip trip = requireTrip(tripId);
        TripMember currentOwner = requireRole(trip, currentUser, TripMemberRole.OWNER);

        if (currentUser.getId().equals(newOwnerUserId)) {
            throw new BadRequestException("You already own this trip");
        }

        User newOwnerUser = userRepo.findById(newOwnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TripMember newOwnerMembership = tripMemberRepo.findByTripAndUser(trip, newOwnerUser)
                .orElseThrow(() -> new BadRequestException("User is not a trip member"));

        // Single-owner guarantee: the previous owner becomes an admin (FR-TRIP-002).
        currentOwner.setRole(TripMemberRole.ADMIN);
        newOwnerMembership.setRole(TripMemberRole.OWNER);
        tripMemberRepo.save(currentOwner);
        tripMemberRepo.save(newOwnerMembership);

        auditLogService.logAction(trip, "TRANSFER_OWNERSHIP", "Trip", trip.getId(),
                "Transferred ownership to: " + newOwnerUser.getName());
        notificationService.createNotification(newOwnerUser, trip, NotificationType.ROLE_CHANGED,
                "You're the trip owner now",
                currentUser.getName() + " made you the owner of \"" + trip.getTitle() + "\"",
                "/trips/" + trip.getId());
    }

    // ---------- invitations ----------

    @Transactional
    public InvitationResponse createInvitation(UUID tripId, CreateInvitationRequest request) {
        User currentUser = getCurrentUser();
        Trip trip = requireTrip(tripId);
        TripMember actor = requireRole(trip, currentUser, TripMemberRole.ADMIN);

        TripRole role = request.role() == null ? TripRole.MEMBER : request.role();
        if (role == TripRole.OWNER) {
            throw new BadRequestException("Invitations can't grant the owner role");
        }
        if (role == TripRole.ADMIN && actor.getRole() != TripMemberRole.OWNER) {
            throw new UnauthorizedException("Only the owner can invite admins");
        }

        int usageLimit = request.usageLimit() == null ? 10 : request.usageLimit();
        if (usageLimit < 1 || usageLimit > 100) {
            throw new BadRequestException("usageLimit must be between 1 and 100");
        }

        String email = request.email() == null ? null : request.email().trim();
        if (email != null && !email.isEmpty() && !EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Enter a valid email address");
        }

        String rawToken = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        TripInvitation saved = tripInvitationRepo.save(new TripInvitation(
                trip,
                currentUser,
                TokenHashUtil.hash(rawToken),
                role,
                expiresAt,
                usageLimit
        ));

        String inviteLink = frontendUrl + "/invite?token=" + rawToken;

        if (email != null && !email.isEmpty()) {
            emailService.sendTripInvitation(email, currentUser.getName(), trip.getTitle(), inviteLink);
        }

        return new InvitationResponse(saved.getId(), inviteLink, expiresAt);
    }

    @Transactional
    public void acceptInvitation(String token) {
        User currentUser = getCurrentUser();

        if (token == null || token.isBlank()) {
            throw new BadRequestException("Invitation token is required");
        }

        TripInvitation invitation = tripInvitationRepo.findByTokenHash(TokenHashUtil.hash(token.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (Boolean.TRUE.equals(invitation.getIsRevoked())) {
            throw new BadRequestException("This invitation was revoked");
        }
        if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("This invitation has expired");
        }
        if (invitation.getUsageCount() >= invitation.getUsageLimit()) {
            throw new BadRequestException("This invitation has been used up");
        }

        Trip trip = invitation.getTrip();
        if (tripMemberRepo.existsByTripAndUser(trip, currentUser)) {
            throw new BadRequestException("You're already a member of this trip");
        }

        tripMemberRepo.save(new TripMember(trip, currentUser, TripMemberRole.valueOf(invitation.getRole().name())));

        invitation.setUsageCount(invitation.getUsageCount() + 1);
        tripInvitationRepo.save(invitation);

        notifyAdmins(trip, currentUser, NotificationType.MEMBER_JOINED, "New trip member",
                currentUser.getName() + " joined \"" + trip.getTitle() + "\"");
    }

    @Transactional
    public void revokeInvitation(UUID invitationId) {
        TripInvitation invitation = tripInvitationRepo.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        requireRole(invitation.getTrip(), getCurrentUser(), TripMemberRole.ADMIN);

        invitation.setIsRevoked(true);
        tripInvitationRepo.save(invitation);
    }

    @Transactional
    public List<InvitationSummaryResponse> getInvitations(UUID tripId) {
        Trip trip = requireTrip(tripId);
        requireRole(trip, getCurrentUser(), TripMemberRole.ADMIN);

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

    // ---------- helpers ----------

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Trip requireTrip(UUID tripId) {
        return tripRepo.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    // OWNER > ADMIN > MEMBER; the enum is declared in that order.
    private TripMember requireRole(Trip trip, User user, TripMemberRole minimum) {
        TripMember member = tripMemberRepo.findByTripAndUser(trip, user)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this trip"));
        if (member.getRole().ordinal() > minimum.ordinal()) {
            throw new UnauthorizedException(
                    "This needs the " + minimum.name().toLowerCase() + " role or higher");
        }
        return member;
    }

    private void notifyMembers(Trip trip, User actor, NotificationType type, String title, String message) {
        for (TripMember m : tripMemberRepo.findByTrip(trip)) {
            if (!m.getUser().getId().equals(actor.getId())) {
                notificationService.createNotification(m.getUser(), trip, type, title, message, "/trips/" + trip.getId());
            }
        }
    }

    private void notifyAdmins(Trip trip, User actor, NotificationType type, String title, String message) {
        for (TripMember m : tripMemberRepo.findByTrip(trip)) {
            if (m.getRole() != TripMemberRole.MEMBER && !m.getUser().getId().equals(actor.getId())) {
                notificationService.createNotification(m.getUser(), trip, type, title, message, "/trips/" + trip.getId());
            }
        }
    }

    private String cleanCoverUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.length() > 1000) {
            throw new BadRequestException("Cover image URL is too long");
        }
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://") && !trimmed.startsWith("/images/")) {
            throw new BadRequestException("Cover image must be an http(s) URL");
        }
        return trimmed;
    }

    private static String article(TripMemberRole role) {
        return role == TripMemberRole.ADMIN ? "an" : "a";
    }

    private static void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private TripResponse toResponse(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCreatedBy().getName(),
                trip.getCoverImageUrl()
        );
    }
}
