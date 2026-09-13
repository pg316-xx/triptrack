package com.tripnext.demo.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.notification.NotificationResponse;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.model.Notification;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.NotificationType;
import com.tripnext.demo.repo.NotificationRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepo notificationRepo;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate;

    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        
        return notificationRepo
                .findByUserOrderByCreatedAtDesc(currentUser, pageable)
                .map(this::map);
    }

    public long getUnreadCount() {
        User currentUser = currentUserService.getCurrentUser();
        return notificationRepo.countByUserAndIsReadFalse(currentUser);
    }

    @Transactional
    public void markAsRead(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        int updated = notificationRepo.markAsRead(id, currentUser);
        
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found");
        }
    }

    @Transactional
    public void markAllAsRead() {
        User currentUser = currentUserService.getCurrentUser();
        notificationRepo.markAllAsRead(currentUser);
    }

    @Transactional
    public void createNotification(
            User targetUser,
            Trip trip,
            NotificationType type,
            String title,
            String message,
            String actionUrl
    ) {
        Notification notification = new Notification(
                targetUser,
                trip,
                type,
                title,
                message,
                actionUrl
        );

        Notification saved = notificationRepo.save(notification);
        
        NotificationResponse response = map(saved);

        // Broadcast to user's private topic
        messagingTemplate.convertAndSend(
                "/topic/user/" + targetUser.getId() + "/notifications",
                response
        );
    }

    private NotificationResponse map(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTrip() != null ? notification.getTrip().getId() : null,
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionUrl(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
