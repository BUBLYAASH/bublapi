package org.bublapi.dent.patient.mapper;

import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.patient.dto.PatientResponseDto;
import org.bublapi.dent.patient.dto.UpdatePatientRequestDto;
import org.bublapi.dent.patient.entity.Patient;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface PatientMapper {

   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "user", ignore = true)
   Patient toEntity(CreatePatientRequestDto request);

   @Mapping(target = "clinicId", source = "clinic.id")
   @Mapping(target = "userId", source = "user.id")
   PatientResponseDto toResponse(Patient entity);

   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   void updateEntity(UpdatePatientRequestDto request, @MappingTarget Patient patient);
}
