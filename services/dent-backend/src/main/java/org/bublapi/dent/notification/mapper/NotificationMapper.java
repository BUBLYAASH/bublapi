package org.bublapi.dent.notification.mapper;

import org.bublapi.dent.notification.dto.NotificationResponseDto;
import org.bublapi.dent.notification.dto.UserNotificationResponseDto;
import org.bublapi.dent.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
   @Mapping(target = "clinicId", source = "clinic.id")
   @Mapping(target = "userId", source = "user.id")
   @Mapping(target = "appointmentId", source = "appointment.id")
   NotificationResponseDto toResponse(Notification notification);

   @Mapping(target = "userId", source = "user.id")
   @Mapping(target = "appointmentId", source = "appointment.id")
   UserNotificationResponseDto toUserResponse(Notification notification);
}
