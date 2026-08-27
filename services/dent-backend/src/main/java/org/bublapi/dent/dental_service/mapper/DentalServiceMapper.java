package org.bublapi.dent.dental_service.mapper;

import org.bublapi.dent.dental_service.dto.CreateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.dto.DentalServiceResponseDto;
import org.bublapi.dent.dental_service.dto.UpdateDentalServiceRequestDto;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DentalServiceMapper {
   DentalService toEntity(CreateDentalServiceRequestDto request);

   DentalServiceResponseDto toResponse(DentalService entity);

   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   void updateEntity(UpdateDentalServiceRequestDto request, @MappingTarget DentalService entity);
}
