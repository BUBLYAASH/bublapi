package org.bublapi.dent.clinic.mapper;

import org.bublapi.dent.clinic.dto.ClinicResponseDto;
import org.bublapi.dent.clinic.dto.CreateClinicRequestDto;
import org.bublapi.dent.clinic.dto.UpdateClinicRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ClinicMapper {

  Clinic toEntity(CreateClinicRequestDto request);

  ClinicResponseDto toResponse(Clinic entity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateEntity(UpdateClinicRequestDto request, @MappingTarget Clinic clinic);
}
