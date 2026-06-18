package org.bublapi.dent.doctor.mapper;

import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.entity.Doctor;
import org.mapstruct.*;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DoctorMapper {
  @Mapping(target = "clinic", ignore = true)
  Doctor toEntity(CreateDoctorRequestDto response);

  @Mapping(target = "clinicId", source = "clinic.id")
  DoctorResponseDto toResponse(Doctor entity);

  @Mapping(target = "clinic", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateEntity(UpdateDoctorRequestDto request, @MappingTarget Doctor doctor);
}
