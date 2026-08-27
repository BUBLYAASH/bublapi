package org.bublapi.dent.notification.dto;

import org.bublapi.dent.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserNotificationResponseDto(
        UUID id, UUID userId, UUID appointmentId, NotificationType type, String title, String message,
        LocalDateTime sentAt) {
}
