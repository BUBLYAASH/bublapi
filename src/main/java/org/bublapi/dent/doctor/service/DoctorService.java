package org.bublapi.dent.doctor.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.LinkUserToDoctorRequestDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.mapper.DoctorMapper;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DoctorService {

   private final DoctorRepository doctorRepository;
   private final ClinicRepository clinicRepository;
   private final UserRepository userRepository;
   private final DoctorMapper doctorMapper;

   public DoctorService(DoctorRepository doctorRepository, ClinicRepository clinicRepository, UserRepository userRepository, DoctorMapper doctorMapper) {
      this.doctorRepository = doctorRepository;
      this.clinicRepository = clinicRepository;
      this.userRepository = userRepository;
      this.doctorMapper = doctorMapper;
   }

   public DoctorResponseDto create(UUID clinicId, CreateDoctorRequestDto request) {
      Clinic clinic = clinicRepository.findById(clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      Doctor entity = doctorMapper.toEntity(request);
      entity.setClinic(clinic);
      Doctor saved = doctorRepository.save(entity);
      return doctorMapper.toResponse(saved);
   }

   @Transactional
   public DoctorResponseDto update(UUID clinicId, UUID doctorId, UpdateDoctorRequestDto request) {
      Doctor doctor = doctorRepository.findByIdAndClinic_Id(doctorId, clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor in clinic not found"));

      doctorMapper.updateEntity(request, doctor);

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto deactivate(UUID clinicId, UUID doctorId) {
      Doctor doctor = doctorRepository.findByIdAndClinic_Id(doctorId, clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found in this clinic"));

      doctor.setActive(false);

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto activate(UUID clinicId, UUID doctorId) {
      Doctor doctor = doctorRepository.findByIdAndClinic_Id(doctorId, clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found in this clinic"));

      doctor.setActive(true);

      return doctorMapper.toResponse(doctor);
   }

   public List<DoctorResponseDto> findAll(UUID clinicId) {
      clinicRepository.findByIdAndActiveTrue(clinicId)
                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      return doctorRepository.findAllByClinic_IdAndActiveTrue(clinicId).stream().map(doctorMapper::toResponse).toList();
   }

   @Transactional
   public DoctorResponseDto linkUser(UUID clinicId, UUID doctorId, LinkUserToDoctorRequestDto request) {
      if ((request.email() == null || request.email().isBlank()) && (request.phone() == null || request.phone()
                                                                                                       .isBlank())) {
         throw new BadRequestException("Email and phone are empty");
      }

      User user = userRepository.findByEmailOrPhoneInClinic(clinicId, request.email(), request.phone())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      Doctor doctor = doctorRepository.findAvailableDoctorInClinic(clinicId, doctorId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      if (doctor.getUser() != null) {
         throw new BadRequestException("User is already linked to doctor's profile");
      }

      doctorRepository.findByUser_Id(user.getId()).ifPresent(e -> {
         throw new ResourceNotFoundException("User not found");
      });

      doctor.setUser(user);

      return doctorMapper.toResponse(doctor);
   }
}
