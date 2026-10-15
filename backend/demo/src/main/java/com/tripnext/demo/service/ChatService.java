package com.tripnext.demo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.chat.ChatMessageResponse;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.ChatMessage;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.TripMemberRole;
import com.tripnext.demo.repo.ChatMessageRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepo chatMessageRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final CurrentUserService currentUserService;

    @Transactional
    public ChatMessageResponse saveMessage(
            UUID tripId,
            String message
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        Trip trip = getTrip(tripId);

        validateMembership(trip, currentUser);

        validateMessage(message);

        ChatMessage chatMessage =
                new ChatMessage(
                        trip,
                        currentUser,
                        message
                );

        ChatMessage saved =
                chatMessageRepo.save(chatMessage);

        return map(saved);
    }

@Transactional
    public List<ChatMessageResponse> getMessages(
            UUID tripId
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        Trip trip = getTrip(tripId);

        validateMembership(trip, currentUser);

        return chatMessageRepo
                .findByTripOrderByCreatedAtAsc(trip)
                .stream()
                .map(this::map)
                .toList();
    }

@Transactional
    public Page<ChatMessageResponse> getMessages(
            UUID tripId,
            Pageable pageable
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        Trip trip = getTrip(tripId);

        validateMembership(trip, currentUser);

        return chatMessageRepo
                .findByTripOrderByCreatedAtDesc(
                        trip,
                        pageable
                )
                .map(this::map);
    }

    @Transactional
    public void deleteMessage(
            UUID tripId,
            UUID messageId
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        Trip trip = getTrip(tripId);

        ChatMessage message =
                chatMessageRepo.findById(messageId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message not found"
                                )
                        );

        if (!message.getTrip().getId()
                .equals(trip.getId())) {

            throw new BadRequestException(
                    "Message does not belong to this trip"
            );
        }

        boolean isSender =
                message.getSender().getId()
                        .equals(currentUser.getId());

        boolean isOwnerOrAdmin =
                tripMemberRepo.findByTripAndUser(
                        trip,
                        currentUser
                )
                .map(member ->
                        member.getRole() == TripMemberRole.OWNER
                        || member.getRole() == TripMemberRole.ADMIN
                )
                .orElse(false);

        if (!isSender && !isOwnerOrAdmin) {
            throw new UnauthorizedException(
                    "Only the sender or trip owner/admin can delete messages"
            );
        }

        chatMessageRepo.delete(message);
    }

    private Trip getTrip(UUID tripId) {

        return tripRepo.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip not found"
                        )
                );
    }

    private void validateMembership(
            Trip trip,
            User user
    ) {

        boolean isMember =
                tripMemberRepo.existsByTripAndUser(
                        trip,
                        user
                );

        if (!isMember) {
            throw new UnauthorizedException(
                    "Not a trip member"
            );
        }
    }

    private void validateMessage(String message) {

        if (message == null
                || message.isBlank()) {

            throw new BadRequestException(
                    "Message cannot be empty"
            );
        }

        if (message.length() > 2000) {
            throw new BadRequestException(
                    "Message cannot exceed 2000 characters"
            );
        }
    }

    private ChatMessageResponse map(
            ChatMessage message
    ) {

        return new ChatMessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getMessage(),
                message.getCreatedAt()
        );
    }
}