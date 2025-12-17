package com.banking.core.notification.repository;

import com.banking.core.notification.module.entity.Notification;
import com.banking.core.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);

    List<Notification> findByIsRead(Boolean isRead);

    List<Notification> findByUserIdAndIsRead(Long userId, Boolean isRead);

    List<Notification> findByNotificationType(NotificationType notificationType);

    List<Notification> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdDate DESC")
    List<Notification> findAllByUserIdOrderByCreatedDateDesc(@Param("userId") Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
