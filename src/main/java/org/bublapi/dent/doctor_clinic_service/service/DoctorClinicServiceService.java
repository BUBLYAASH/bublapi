package org.bublapi.dent.doctor_clinic_service.service;

import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.doctor_clinic_service.dto.DoctorClinicServiceResponseDto;
import org.bublapi.dent.doctor_clinic_service.entity.DoctorClinicService;
import org.bublapi.dent.doctor_clinic_service.mapper.DoctorClinicServiceMapper;
import org.bublapi.dent.doctor_clinic_service.repository.DoctorClinicServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DoctorClinicServiceService {
   private final DoctorClinicServiceRepository doctorClinicServiceRepository;
   private final DoctorRepository doctorRepository;
   private final ClinicServiceRepository clinicServiceRepository;
   private final DoctorClinicServiceMapper doctorClinicServiceMapper;

   public DoctorClinicServiceService(DoctorClinicServiceRepository doctorClinicServiceRepository, DoctorRepository doctorRepository, ClinicServiceRepository clinicServiceRepository, DoctorClinicServiceMapper doctorClinicServiceMapper) {
      this.doctorClinicServiceRepository = doctorClinicServiceRepository;
      this.doctorRepository = doctorRepository;
      this.clinicServiceRepository = clinicServiceRepository;
      this.doctorClinicServiceMapper = doctorClinicServiceMapper;
   }

   @Transactional(readOnly = true)
   public List<DoctorClinicServiceResponseDto> findAllByDoctorId(UUID doctorId) {
      doctorRepository.findByIdAndActiveTrue(doctorId)
                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      return doctorClinicServiceRepository.findAllByDoctor_Id(doctorId)
                                          .stream()
                                          .map(doctorClinicServiceMapper::toResponse)
                                          .toList();
   }

   @Transactional(readOnly = true)
   public List<DoctorClinicServiceResponseDto> findAllByClinicServiceId(UUID clinicServiceId) {
      clinicServiceRepository.findByIdAndActiveTrue(clinicServiceId)
                             .orElseThrow(
                                     () -> new ResourceNotFoundException("Clinic Service not found or unavailable"));

      return doctorClinicServiceRepository.findAllByClinicService_Id(clinicServiceId)
                                          .stream()
                                          .map(doctorClinicServiceMapper::toResponse)
                                          .toList();
   }

   @Transactional
   public DoctorClinicServiceResponseDto assignService(UUID doctorId, UUID clinicServiceId) {
      Doctor doctor = doctorRepository.findByIdAndActiveTrue(doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found or unavailable"));

      ClinicService clinicService = clinicServiceRepository.findByIdAndActiveTrue(clinicServiceId)
                                                           .orElseThrow(() -> new ResourceNotFoundException(
                                                                   "Clinic Service not found or unavailable"));

      if (doctorClinicServiceRepository.existsByDoctor_IdAndClinicService_Id(doctorId, clinicServiceId)) {
         throw new BadRequestException("Clinic service for doctor already assigned");
      }

      DoctorClinicService doctorClinicService = new DoctorClinicService();

      doctorClinicService.setDoctor(doctor);
      doctorClinicService.setClinicService(clinicService);

      DoctorClinicService saved = doctorClinicServiceRepository.save(doctorClinicService);

      return doctorClinicServiceMapper.toResponse(saved);
   }

   @Transactional
   public void removeService(UUID doctorId, UUID clinicServiceId) {
      doctorRepository.findById(doctorId)
                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      clinicServiceRepository.findById(clinicServiceId)
                             .orElseThrow(
                                     () -> new ResourceNotFoundException("Clinic Service not found or unavailable"));

      DoctorClinicService doctorClinicService = doctorClinicServiceRepository.findByDoctor_IdAndClinicService_Id(
                                                                                     doctorId, clinicServiceId)
                                                                             .orElseThrow(
                                                                                     () -> new ResourceNotFoundException(
                                                                                             "DoctorClinicService not found or unavailable"));

      doctorClinicServiceRepository.delete(doctorClinicService);
   }
}
