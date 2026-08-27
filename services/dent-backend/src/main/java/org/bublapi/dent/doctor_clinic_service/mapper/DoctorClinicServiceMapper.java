package org.bublapi.dent.doctor_clinic_service.mapper;

import org.bublapi.dent.doctor_clinic_service.dto.DoctorClinicServiceResponseDto;
import org.bublapi.dent.doctor_clinic_service.entity.DoctorClinicService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DoctorClinicServiceMapper {
   @Mapping(target = "doctorId", source = "doctor.id")
   @Mapping(target = "clinicServiceId", source = "clinicService.id")
   @Mapping(target = "dentalServiceId", source = "clinicService.dentalService.id")
   @Mapping(target = "title", source = "clinicService.dentalService.title")
   @Mapping(target = "price", source = "clinicService.price")
   @Mapping(target = "durationMinutes", source = "clinicService.durationMinutes")
   DoctorClinicServiceResponseDto toResponse(DoctorClinicService doctorClinicService);
}
