package org.bublapi.dent.doctor.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.LinkUserToDoctorRequestDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.mapper.DoctorMapper;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.role.repository.RoleRepository;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DoctorService {

   private final DoctorRepository doctorRepository;
   private final UserRepository userRepository;
   private final RoleRepository roleRepository;
   private final DoctorMapper doctorMapper;

   public DoctorService(DoctorRepository doctorRepository, UserRepository userRepository, RoleRepository roleRepository, DoctorMapper doctorMapper) {
      this.doctorRepository = doctorRepository;
      this.userRepository = userRepository;
      this.roleRepository = roleRepository;
      this.doctorMapper = doctorMapper;
   }

   @Transactional
   public DoctorResponseDto create(CreateDoctorRequestDto request) {
      Clinic clinic = ClinicContext.get();

      Doctor entity = doctorMapper.toEntity(request);
      entity.setClinic(clinic);
      Doctor saved = doctorRepository.save(entity);
      return doctorMapper.toResponse(saved);
   }

   @Transactional
   public DoctorResponseDto update(UUID doctorId, UpdateDoctorRequestDto request) {
      Doctor doctor = doctorRepository.findById(doctorId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor in clinic not found"));

      doctorMapper.updateEntity(request, doctor);

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto deactivate(UUID doctorId) {
      Doctor doctor = doctorRepository.findById(doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found in this clinic"));

      doctor.setActive(false);

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto activate(UUID doctorId) {
      Doctor doctor = doctorRepository.findById(doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found in this clinic"));

      doctor.setActive(true);

      return doctorMapper.toResponse(doctor);
   }

   public List<DoctorResponseDto> findAllForStaff() {
      return doctorRepository.findAll().stream().map(doctorMapper::toResponse).toList();
   }

   public List<DoctorResponseDto> findAllActiveForPublic() {
      return doctorRepository.findAllByActiveTrue().stream().map(doctorMapper::toResponse).toList();
   }

   public DoctorResponseDto findById(UUID doctorId) {
      Doctor doctor = doctorRepository.findById(doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found in this clinic"));

      return doctorMapper.toResponse(doctor);
   }

   public DoctorResponseDto findActiveById(UUID doctorId) {
      Doctor doctor = doctorRepository.findByIdAndActiveTrue(doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found or unavailable"));

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto linkUser(UUID doctorId, LinkUserToDoctorRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();
      String email = request.email().trim().toLowerCase(Locale.ROOT);
      String phone = request.phone().trim();

      if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
         throw new BadRequestException("Email and phone are empty");
      }

      User user = userRepository.findByEmailOrPhoneInClinic(email, phone, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      Doctor doctor = doctorRepository.findByIdAndActiveTrue(doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found or unavailable"));

      Role doctorRole = roleRepository.findByName(RoleName.DOCTOR)
                                      .orElseThrow(() -> new ResourceNotFoundException("Role DOCTOR not found"));

      if (doctor.getUser() != null) {
         throw new BadRequestException("Doctor profile is already linked to a user");
      }

      doctorRepository.findByUser_Id(user.getId()).ifPresent(e -> {
         throw new BadRequestException("User is already linked to another doctor profile");
      });

      doctor.setUser(user);
      user.getRoles().add(doctorRole);

      return doctorMapper.toResponse(doctor);
   }

   @Transactional
   public DoctorResponseDto unlinkUser(UUID doctorId) {
      Doctor doctor = doctorRepository.findById(doctorId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

      User user = doctor.getUser();

      if (user == null) {
         throw new BadRequestException("Doctor profile is not linked to a user");
      }

      doctor.setUser(null);

      user.getRoles().removeIf(role -> role.getName() == RoleName.DOCTOR);

      return doctorMapper.toResponse(doctor);
   }
}
