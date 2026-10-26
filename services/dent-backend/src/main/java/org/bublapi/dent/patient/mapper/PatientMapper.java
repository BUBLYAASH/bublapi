package org.bublapi.dent.patient.mapper;

import org.bublapi.dent.patient.dto.CreatePatientFromProfileRequestDto;
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

   @Mapping(target = "id", ignore = true)
   @Mapping(target = "createdAt", ignore = true)
   @Mapping(target = "updatedAt", ignore = true)
   @Mapping(target = "active", ignore = true)
   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "user", ignore = true)
   Patient toEntity(CreatePatientRequestDto request);

   @Mapping(target = "id", ignore = true)
   @Mapping(target = "firstName", ignore = true)
   @Mapping(target = "lastName", ignore = true)
   @Mapping(target = "middleName", ignore = true)
   @Mapping(target = "phone", ignore = true)
   @Mapping(target = "email", ignore = true)
   @Mapping(target = "createdAt", ignore = true)
   @Mapping(target = "updatedAt", ignore = true)
   @Mapping(target = "active", ignore = true)
   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "user", ignore = true)
   Patient toEntity(CreatePatientFromProfileRequestDto request);

   @Mapping(target = "clinicId", source = "clinic.id")
   @Mapping(target = "userId", source = "user.id")
   PatientResponseDto toResponse(Patient entity);

   @Mapping(target = "id", ignore = true)
   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "user", ignore = true)
   @Mapping(target = "createdAt", ignore = true)
   @Mapping(target = "updatedAt", ignore = true)
   @Mapping(target = "active", ignore = true)
   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   void updateEntity(UpdatePatientRequestDto request, @MappingTarget Patient patient);
}
