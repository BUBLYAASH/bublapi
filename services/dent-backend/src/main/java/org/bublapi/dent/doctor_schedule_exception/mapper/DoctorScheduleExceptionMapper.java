package org.bublapi.dent.doctor_schedule_exception.mapper;

import org.bublapi.dent.doctor_schedule_exception.dto.DoctorScheduleExceptionResponseDto;
import org.bublapi.dent.doctor_schedule_exception.dto.SetDoctorScheduleExceptionRequestDto;
import org.bublapi.dent.doctor_schedule_exception.entity.DoctorScheduleException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DoctorScheduleExceptionMapper {
   @Mapping(target = "doctor", ignore = true)
   DoctorScheduleException toEntity(SetDoctorScheduleExceptionRequestDto request);

   @Mapping(target = "doctorId", source = "doctor.id")
   DoctorScheduleExceptionResponseDto toResponse(DoctorScheduleException entity);
}
