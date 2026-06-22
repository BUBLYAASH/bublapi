package org.bublapi.dent.clinic.service;

import org.bublapi.dent.clinic.dto.ClinicResponseDto;
import org.bublapi.dent.clinic.dto.CreateClinicRequestDto;
import org.bublapi.dent.clinic.dto.UpdateClinicRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.mapper.ClinicMapper;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.patient.repository.PatientRepository;
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
   private final PatientRepository patientRepository;
   private final ClinicServiceRepository clinicServiceRepository;
   private final ClinicMapper clinicMapper;

   public ClinicService(ClinicRepository clinicRepository, UserRepository userRepository, PatientRepository patientRepository, ClinicServiceRepository clinicServiceRepository, ClinicMapper clinicMapper) {
      this.clinicRepository = clinicRepository;
      this.userRepository = userRepository;
      this.patientRepository = patientRepository;
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

      List<User> users = userRepository.findAllByClinic_IdAndEnabledTrue(id);

      users.forEach(user -> {
         user.setEnabled(false);
         user.setDisabledByClinic(true);
      });

      patientRepository.findAllByClinic_Id(id).forEach(patient -> patient.setActive(false));

      clinicServiceRepository.findAllByClinic_Id(id).forEach(clinicService -> clinicService.setActive(false));

      return clinicMapper.toResponse(clinic);
   }

   @Transactional
   public ClinicResponseDto activate(UUID id) {
      Clinic clinic = clinicRepository.findById(id)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      clinic.setActive(true);

      List<User> users = userRepository.findAllByClinic_Id(id);

      users.stream().filter(user -> user.getDisabledByClinic() && !user.getEnabled()).forEach(user -> {
         user.setDisabledByClinic(false);
         user.setEnabled(true);
      });

      List<Patient> patients = patientRepository.findAllByClinic_Id(id);

      patients.stream()
              .filter(patient -> patient.getUser() == null || patient.getUser().getEnabled())
              .forEach(patient -> patient.setActive(true));

      clinicServiceRepository.findAllByClinic_Id(id).forEach(clinicService -> clinicService.setActive(true));


      return clinicMapper.toResponse(clinic);
   }

   public List<ClinicResponseDto> findAll() {
      return clinicRepository.findAllByActiveTrue().stream().map(clinicMapper::toResponse).toList();
   }
}
