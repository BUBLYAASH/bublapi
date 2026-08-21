package org.bublapi.dent.clinic.service;

import org.bublapi.dent.clinic.dto.ClinicResponseDto;
import org.bublapi.dent.clinic.dto.CreateClinicRequestDto;
import org.bublapi.dent.clinic.dto.UpdateClinicRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.mapper.ClinicMapper;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClinicService {

   private final ClinicRepository clinicRepository;
   private final UserRepository userRepository;
   private final DoctorRepository doctorRepository;
   private final ClinicServiceRepository clinicServiceRepository;
   private final ClinicMapper clinicMapper;

   public ClinicService(ClinicRepository clinicRepository, UserRepository userRepository, DoctorRepository doctorRepository, ClinicServiceRepository clinicServiceRepository, ClinicMapper clinicMapper) {
      this.clinicRepository = clinicRepository;
      this.userRepository = userRepository;
      this.doctorRepository = doctorRepository;
      this.clinicServiceRepository = clinicServiceRepository;
      this.clinicMapper = clinicMapper;
   }

   public ClinicResponseDto create(CreateClinicRequestDto request) {
      Clinic clinic = clinicMapper.toEntity(request);

      Clinic saved = clinicRepository.save(clinic);

      return clinicMapper.toResponse(saved);
   }

   @Transactional
   public ClinicResponseDto update(UUID id, UpdateClinicRequestDto request) {
      Clinic clinic = clinicRepository.findById(id)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      clinicMapper.updateEntity(request, clinic);

      return clinicMapper.toResponse(clinic);
   }

   @Transactional
   public ClinicResponseDto deactivate(UUID id) {
      Clinic clinic = clinicRepository.findById(id)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      clinic.setActive(false);

      List<User> users = userRepository.findAllByClinic_IdAndEnabledTrue(clinic.getId());
      List<Doctor> doctors = doctorRepository.findAllByClinic_IdAndActiveTrue(clinic.getId());
      List<org.bublapi.dent.clinic_service.entity.ClinicService> clinicServices = clinicServiceRepository.findAllByClinic_IdAndActiveTrue(
              clinic.getId());

      users.forEach(user -> {
         user.setEnabled(false);
         user.setDisabledByClinic(true);
      });

      doctors.forEach(doctor -> {
         doctor.setActive(false);
         doctor.setDisabledByClinic(true);
      });

      clinicServices.forEach(clinicService -> {
         clinicService.setActive(false);
         clinicService.setDisabledByClinic(true);
      });

      return clinicMapper.toResponse(clinic);
   }

   @Transactional
   public ClinicResponseDto activate(UUID id) {
      Clinic clinic = clinicRepository.findById(id)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      clinic.setActive(true);

      List<User> users = userRepository.findAllByClinic_IdAndDisabledByClinicTrue(clinic.getId());
      List<Doctor> doctors = doctorRepository.findAllByClinic_IdAndDisabledByClinicTrue(clinic.getId());
      List<org.bublapi.dent.clinic_service.entity.ClinicService> clinicServices = clinicServiceRepository.findAllByClinic_IdAndDisabledByClinicTrue(
              clinic.getId());

      users.forEach(user -> {
         user.setDisabledByClinic(false);
         user.setEnabled(true);
      });

      doctors.forEach(doctor -> {
         doctor.setDisabledByClinic(false);
         doctor.setActive(true);
      });

      clinicServices.forEach(clinicService -> {
         clinicService.setDisabledByClinic(false);
         clinicService.setActive(true);
      });

      return clinicMapper.toResponse(clinic);
   }

   public List<ClinicResponseDto> findAll() {
      return clinicRepository.findAll().stream().map(clinicMapper::toResponse).toList();
   }
}
