package org.bublapi.dent.doctor.mapper;

import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.entity.Doctor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DoctorMapper {

   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "user", ignore = true)
   Doctor toEntity(CreateDoctorRequestDto response);

   @Mapping(target = "clinicId", source = "clinic.id")
   @Mapping(target = "userId", source = "user.id")
   DoctorResponseDto toResponse(Doctor entity);

   @Mapping(target = "clinic", ignore = true)
   @Mapping(target = "user", ignore = true)
   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   void updateEntity(UpdateDoctorRequestDto request, @MappingTarget Doctor doctor);
}
