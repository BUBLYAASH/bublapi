package org.bublapi.dent.notification.command;

public record UserNotificationData(String clinicTitle, String firstName) implements NotificationData {
}
