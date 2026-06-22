package org.bublapi.dent.appointment_service.mapper;

import org.bublapi.dent.appointment_service.dto.AppointmentServiceResponseDto;
import org.bublapi.dent.appointment_service.entity.AppointmentServiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentServiceMapper {
   @Mapping(target = "clinicServiceId", source = "clinicService.id")
   AppointmentServiceResponseDto toResponse(AppointmentServiceItem entity);
}
