package com.banking.core.notification.extra.provider;

import com.banking.core.notification.module.entity.Notification;
import com.banking.core.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notification Query Provider
 *
 * Encapsulates read-only queries for Notification domain entities.
 * Provides optimized access to notification-related data without business logic.
 * Returns domain entities for use by facades and other query providers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryProvider {

    private final NotificationRepository notificationRepository;

    /**
     * Find unread notifications for a user
     *
     * @param userId the user ID to search for
     * @return List of unread Notification entities for the user
     */
    public List<Notification> findUnreadNotificationsByUserId(Long userId) {
        log.debug("Finding unread notifications for user ID: {}", userId);
        return notificationRepository.findByUserIdAndIsRead(userId, false);
    }

    /**
     * Find all notifications for a user
     *
     * @param userId the user ID to search for
     * @return List of all Notification entities for the user ordered by created date descending
     */
    public List<Notification> findAllNotificationsByUserId(Long userId) {
        log.debug("Finding all notifications for user ID: {}", userId);
        return notificationRepository.findAllByUserIdOrderByCreatedDateDesc(userId);
    }

    /**
     * Count unread notifications for a user
     *
     * @param userId the user ID to search for
     * @return count of unread notifications
     */
    public long countUnreadNotificationsByUserId(Long userId) {
        log.debug("Counting unread notifications for user ID: {}", userId);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Find notifications by type
     *
     * @param notificationType the notification type to search for
     * @return List of Notification entities matching the type
     */
    public List<Notification> findNotificationsByType(com.banking.core.enums.NotificationType notificationType) {
        log.debug("Finding notifications by type: {}", notificationType);
        return notificationRepository.findByNotificationType(notificationType);
    }
}
