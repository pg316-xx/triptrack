package com.tripnext.demo.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tripnext.demo.dto.audit.AuditLogResponse;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.AuditLog;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.TripMemberRole;
import com.tripnext.demo.repo.AuditLogRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepo auditLogRepo;
    private final CurrentUserService currentUserService;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;

    /**
     * Fetch audit logs for a trip. Only OWNER and ADMIN can access.
     */
    public Page<AuditLogResponse> getTripAuditLogs(UUID tripId, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();

        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        TripMember member = tripMemberRepo.findByTripAndUser(trip, currentUser)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this trip"));

        if (member.getRole() == TripMemberRole.MEMBER) {
            throw new UnauthorizedException("Only Owners and Admins can view audit logs");
        }

        return auditLogRepo
                .findByTripOrderByCreatedAtDesc(trip, pageable)
                .map(this::map);
    }

    /**
     * Save an audit log. Runs in a new transaction so it commits even if the parent transaction fails,
     * or at least avoids holding up the parent transaction if logging fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
            Trip trip,
            String action,
            String entityType,
            UUID entityId,
            String metadata
    ) {
        try {
            User currentUser = currentUserService.getCurrentUser();

            AuditLog log = new AuditLog(
                    currentUser,
                    trip,
                    action,
                    entityType,
                    entityId,
                    metadata
            );

            auditLogRepo.save(log);
        } catch (Exception e) {
            // Log the error but don't fail the main business logic
            System.err.println("Failed to save audit log: " + e.getMessage());
        }
    }

    private AuditLogResponse map(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUser().getId(),
                log.getUser().getName(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getMetadata(),
                log.getCreatedAt()
        );
    }
}
