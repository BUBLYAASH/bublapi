package org.bublapi.dent.dental_service.mapper;

import org.bublapi.dent.dental_service.dto.CreateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.dto.DentalServiceResponseDto;
import org.bublapi.dent.dental_service.dto.UpdateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DentalServiceMapper {
   DentalService toEntity(CreateDentalServiceRequestDto request);

   DentalServiceResponseDto toResponse(DentalService entity);

   void updateEntity(UpdateDentalServiceRequestDto request, @MappingTarget DentalService entity);
}
