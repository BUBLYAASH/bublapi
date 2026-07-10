package org.bublapi.dent.notification.command;

import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateNotificationCommand(
        UUID clinicId, UUID userId, UUID appointmentId, NotificationType type, NotificationChannel channel,
        String recipientEmail, String title, String message, LocalDateTime scheduledAt) {
}
