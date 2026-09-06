package org.bublapi.dent.notification.command;

public record PatientCardNotificationData(String clinicTitle, String firstName) implements NotificationData {
}
