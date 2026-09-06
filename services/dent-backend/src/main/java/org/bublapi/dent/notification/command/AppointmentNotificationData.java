package org.bublapi.dent.notification.command;

import java.util.List;

public record AppointmentNotificationData(
        String clinicTitle, String firstName, String scheduledAt, String doctorName,
        List<String> serviceTitles) implements NotificationData {
}
