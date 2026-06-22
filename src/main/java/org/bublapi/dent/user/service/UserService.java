package org.bublapi.dent.user.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.repository.RoleRepository;
import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.UpdateUserRequestDto;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.dto.UserRoleResponseDto;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.mapper.UserMapper;
import org.bublapi.dent.user.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

   private final UserRepository userRepository;
   private final ClinicRepository clinicRepository;
   private final RoleRepository roleRepository;
   private final PatientRepository patientRepository;
   private final UserMapper userMapper;

   public UserService(UserRepository userRepository, ClinicRepository clinicRepository, RoleRepository roleRepository, PatientRepository patientRepository, UserMapper userMapper) {
      this.userRepository = userRepository;
      this.clinicRepository = clinicRepository;
      this.roleRepository = roleRepository;
      this.patientRepository = patientRepository;
      this.userMapper = userMapper;
   }

   @Transactional
   public UserResponseDto create(UUID clinicId, CreateUserRequestDto request) {
      Clinic clinic = clinicRepository.findById(clinicId).orElseThrow(() -> new ResourceNotFoundException("Clinic with provided ID not found"));

      String hash = BCrypt.hashpw(request.password(), BCrypt.gensalt(12));

      User user = userMapper.toEntity(request);
      user.setClinic(clinic);
      user.setPasswordHash(hash);

      User saved = userRepository.save(user);

      patientRepository.findByEmailOrPhoneInClinic(clinicId, request.email(), request.phone()).ifPresent(patient -> {
         if (patient.getUser() == null) {
            patient.setUser(saved);
         }
      });

      return userMapper.toResponse(saved);
   }

   @Transactional
   public UserResponseDto update(UUID clinicId, UUID userId, UpdateUserRequestDto request) {
      User user = userRepository.findByIdAndClinic_Id(userId, clinicId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

      userMapper.updateEntity(request, user);

      if (request.password() != null && !request.password().isBlank()) {
         String hash = BCrypt.hashpw(request.password(), BCrypt.gensalt(12));
         user.setPasswordHash(hash);
      }

      return userMapper.toResponse(user);
   }

   @Transactional
   public UserRoleResponseDto assignRole(UUID clinicId, UUID userId, UUID roleId) {
      User user = userRepository.findByIdAndClinic_Id(userId, clinicId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
      Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

      boolean added = user.getRoles().add(role);

      if (!added) {
         throw new RuntimeException("User already has this role");
      }

      return new UserRoleResponseDto(user.getId(), role.getId());
   }

   @Transactional
   public UserRoleResponseDto removeRole(UUID clinicId, UUID userId, UUID roleId) {
      User user = userRepository.findByIdAndClinic_Id(clinicId, userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
      Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

      boolean removed = user.getRoles().remove(role);

      if (!removed) {
         throw new ResourceNotFoundException("User does not have this role");
      }

      return new UserRoleResponseDto(user.getId(), role.getId());
   }

   @Transactional
   public UserResponseDto deactivate(UUID clinicId, UUID userId) {
      User user = userRepository.findByIdAndClinic_Id(userId, clinicId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

      user.setEnabled(false);
      user.setDisabledByClinic(false);

      return userMapper.toResponse(user);
   }

   @Transactional
   public UserResponseDto activate(UUID clinicId, UUID userId) {
      User user = userRepository.findByIdAndClinic_Id(userId, clinicId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

      user.setEnabled(true);
      user.setDisabledByClinic(false);
      return userMapper.toResponse(user);
   }

   public List<UserResponseDto> findAll(UUID clinicId) {
      clinicRepository.findByIdAndActiveTrue(clinicId).orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      return userRepository.findAllByClinic_IdAndEnabledTrue(clinicId).stream().map(userMapper::toResponse).toList();
   }
}
