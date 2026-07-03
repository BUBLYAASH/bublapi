package org.bublapi.dent.user.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.role.repository.RoleRepository;
import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.UpdateUserRequestDto;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.dto.UserRoleResponseDto;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.mapper.UserMapper;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

   private final UserRepository userRepository;
   private final ClinicRepository clinicRepository;
   private final RoleRepository roleRepository;
   private final PatientRepository patientRepository;
   private final UserMapper userMapper;
   private final PasswordEncoder passwordEncoder;

   public UserService(UserRepository userRepository, ClinicRepository clinicRepository, RoleRepository roleRepository, PatientRepository patientRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
      this.userRepository = userRepository;
      this.clinicRepository = clinicRepository;
      this.roleRepository = roleRepository;
      this.patientRepository = patientRepository;
      this.userMapper = userMapper;
      this.passwordEncoder = passwordEncoder;
   }

   @Transactional
   public UserResponseDto create(UUID clinicId, CreateUserRequestDto request) {
      Clinic clinic = clinicRepository.findByIdAndActiveTrue(clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic with provided ID not found"));

      Role patientRole = roleRepository.findByName("PATIENT")
                                       .orElseThrow(() -> new ResourceNotFoundException("PATIENT role not found"));

      User user = userMapper.toEntity(request);
      user.setClinic(clinic);
      user.setPasswordHash(passwordEncoder.encode(request.password()));
      user.setRoles(Set.of(patientRole));

      User saved = userRepository.save(user);

      patientRepository.findByEmailOrPhoneInClinic(clinicId, request.email(), request.phone()).ifPresent(patient -> {
         if (patient.getUser() == null) {
            patient.setUser(saved);
         } else {
            throw new BadRequestException("Patient Card was found by provided email or phone, but it assigned to another account. Please, contact receptionist or email us to solve this problem.");
         }
      });

      return userMapper.toResponse(saved);
   }

   @Transactional
   public UserResponseDto update(UUID clinicId, UUID userId, UpdateUserRequestDto request) {
      User user = userRepository.findByIdAndClinic_Id(userId, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      userMapper.updateEntity(request, user);

      if (request.password() != null && !request.password().isBlank()) {
         user.setPasswordHash(passwordEncoder.encode(request.password()));
      }

      return userMapper.toResponse(user);
   }

   @Transactional
   public UserRoleResponseDto assignRole(UUID clinicId, UUID actorUserId, UUID targetUserId, UUID roleId) {
      User actorUser = userRepository.findByIdAndClinic_Id(actorUserId, clinicId)
                                     .orElseThrow(() -> new ResourceNotFoundException("Actor user not found"));

      User targetUser = userRepository.findByIdAndClinic_Id(targetUserId, clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

      Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

      RoleName roleName = role.getName();

      boolean allowed = actorUser.getRoles().stream().anyMatch(r -> r.getName().canAssign(roleName));

      if (!allowed) {
         throw new AccessDeniedException("You cannot assign this role");
      }

      boolean added = targetUser.getRoles().add(role);

      if (!added) {
         throw new BadRequestException("User already has this role");
      }

      return new UserRoleResponseDto(targetUser.getId(), role.getId());
   }

   @Transactional
   public UserRoleResponseDto removeRole(UUID clinicId, UUID actorUserId, UUID targetUserId, UUID roleId) {
      User actorUser = userRepository.findByIdAndClinic_Id(actorUserId, clinicId)
                                     .orElseThrow(() -> new ResourceNotFoundException("Actor user not found"));

      User targetUser = userRepository.findByIdAndClinic_Id(targetUserId, clinicId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

      Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

      RoleName roleName = role.getName();

      boolean allowed = actorUser.getRoles().stream().anyMatch(r -> r.getName().canRemove(roleName));

      if (!allowed) {
         throw new AccessDeniedException("You cannot remove this role");
      }

      if (role.getName().equals(RoleName.PATIENT) && !actorUser.getRoles()
                                                               .stream()
                                                               .anyMatch(r -> r.getName() == RoleName.ADMIN)) {
         throw new AccessDeniedException("You cannot remove PATIENT role");
      }

      boolean removed = targetUser.getRoles().remove(role);

      if (!removed) {
         throw new ResourceNotFoundException("User does not have this role");
      }

      return new UserRoleResponseDto(targetUser.getId(), role.getId());
   }

   @Transactional
   public UserResponseDto deactivate(UUID clinicId, UUID userId) {
      User user = userRepository.findByIdAndClinic_Id(userId, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      user.setEnabled(false);
      user.setDisabledByClinic(false);

      return userMapper.toResponse(user);
   }

   @Transactional
   public UserResponseDto activate(UUID clinicId, UUID userId) {
      User user = userRepository.findByIdAndClinic_Id(userId, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      user.setEnabled(true);
      user.setDisabledByClinic(false);
      return userMapper.toResponse(user);
   }

   public List<UserResponseDto> findAll(UUID clinicId) {
      clinicRepository.findByIdAndActiveTrue(clinicId)
                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

      return userRepository.findAllByClinic_Id(clinicId).stream().map(userMapper::toResponse).toList();
   }
}
