package org.bublapi.dent.doctor_working_hours.mapper;

import org.bublapi.dent.doctor_working_hours.dto.DoctorWorkingHoursResponseDto;
import org.bublapi.dent.doctor_working_hours.dto.SetDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.dto.UpdateDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DoctorWorkingHoursMapper {
   @Mapping(target = "doctor", ignore = true)
   DoctorWorkingHours toEntity(SetDoctorWorkingHoursRequestDto request);

   @Mapping(target = "doctorId", source = "doctor.id")
   DoctorWorkingHoursResponseDto toResponse(DoctorWorkingHours entity);

   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   void updateEntity(UpdateDoctorWorkingHoursRequestDto request, @MappingTarget DoctorWorkingHours entity);
}
