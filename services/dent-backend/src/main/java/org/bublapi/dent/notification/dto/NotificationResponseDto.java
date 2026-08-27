package org.bublapi.dent.notification.dto;

import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationStatus;
import org.bublapi.dent.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponseDto(
        UUID id, UUID clinicId, UUID userId, UUID appointmentId, NotificationType type, NotificationChannel channel,
        String title, String message, NotificationStatus status, LocalDateTime scheduledAt, LocalDateTime sentAt,
        LocalDateTime createdAt) {
}
