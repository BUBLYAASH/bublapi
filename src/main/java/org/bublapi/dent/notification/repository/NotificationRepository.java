package org.bublapi.dent.notification.repository;

import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
   Optional<Notification> findByIdAndUser_IdAndChannelAndStatusAndDeletedFalse(UUID notificationId, UUID userId, NotificationChannel channel, NotificationStatus status);

   List<Notification> findAllByUser_IdAndChannelAndStatusAndReadFalseAndDeletedFalse(UUID userId, NotificationChannel channel, NotificationStatus status);

   long countByUser_IdAndChannelAndStatusAndReadFalseAndDeletedFalse(UUID userId, NotificationChannel channel, NotificationStatus status);

   List<Notification> findAllByUser_IdAndChannelAndStatusAndDeletedFalseOrderBySentAtDesc(UUID userId, NotificationChannel channel, NotificationStatus status);

   List<Notification> findAllByOrderByCreatedAtDesc();
}
