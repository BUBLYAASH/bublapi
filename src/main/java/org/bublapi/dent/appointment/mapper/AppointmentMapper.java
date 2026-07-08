package org.bublapi.dent.appointment.mapper;

import org.bublapi.dent.appointment.dto.AppointmentResponseDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment_service.mapper.AppointmentServiceMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AppointmentServiceMapper.class)
public interface AppointmentMapper {
   @Mapping(target = "id", ignore = true)
   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "patient", ignore = true)
   @Mapping(target = "doctor", ignore = true)
   @Mapping(target = "services", ignore = true)
   @Mapping(target = "endAt", ignore = true)
   @Mapping(target = "totalPrice", ignore = true)
   @Mapping(target = "status", ignore = true)
   @Mapping(target = "createdAt", ignore = true)
   Appointment toEntity(CreateAppointmentRequestDto request);

   @Mapping(target = "clinicId", source = "clinic.id")
   @Mapping(target = "patientId", source = "patient.id")
   @Mapping(target = "doctorId", source = "doctor.id")
   AppointmentResponseDto toResponse(Appointment entity);
}
