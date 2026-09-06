package org.bublapi.dent.notification.command;

public record ClinicServiceNotificationData(
        String clinicTitle, String firstName, String serviceTitle) implements NotificationData {
}
