package org.bublapi.dent.notification.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "dataType")
@JsonSubTypes({@JsonSubTypes.Type(value = AppointmentNotificationData.class, name = "appointment"),
               @JsonSubTypes.Type(value = UserNotificationData.class, name = "user"),
               @JsonSubTypes.Type(value = ClinicServiceNotificationData.class, name = "clinicService")})
public sealed interface NotificationData permits AppointmentNotificationData, UserNotificationData, ClinicServiceNotificationData {
}
