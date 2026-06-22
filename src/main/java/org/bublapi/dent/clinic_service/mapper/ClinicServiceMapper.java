package org.bublapi.dent.clinic_service.mapper;

import org.bublapi.dent.clinic_service.dto.AddClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.dto.ClinicServiceResponseDto;
import org.bublapi.dent.clinic_service.dto.UpdateClinicServiceRequestDto;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClinicServiceMapper {
   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "dentalService", ignore = true)
   ClinicService toEntity(AddClinicServiceRequestDto request);

   @Mapping(target = "clinicId", source = "clinic.id")
   @Mapping(target = "dentalServiceId", source = "dentalService.id")
   ClinicServiceResponseDto toResponse(ClinicService entity);

   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "dentalService", ignore = true)
   void updateEntity(UpdateClinicServiceRequestDto request, @MappingTarget ClinicService entity);
}
