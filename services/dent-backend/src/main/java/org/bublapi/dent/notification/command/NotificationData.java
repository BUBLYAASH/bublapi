package org.bublapi.dent.notification.command;

public sealed interface NotificationData permits AppointmentNotificationData, UserNotificationData, PatientCardNotificationData, ClinicServiceNotificationData {
}
