package org.bublapi.dent.user.service;

import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.repository.RoleRepository;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.dto.UserRoleResponseDto;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.mapper.UserMapper;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService {

   private final UserRepository userRepository;
   private final RoleRepository roleRepository;
   private final UserMapper userMapper;

   public AdminUserService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
      this.userRepository = userRepository;
      this.roleRepository = roleRepository;
      this.userMapper = userMapper;
   }

   @Transactional(readOnly = true)
   public List<UserResponseDto> findAll() {
      return userRepository.findAll()
                           .stream()
                           .filter(user -> user.getClinic() != null)
                           .sorted(Comparator.comparing(user -> String.valueOf(user.getEmail()),
                                                        String.CASE_INSENSITIVE_ORDER))
                           .map(userMapper::toResponse)
                           .toList();
   }

   @Transactional(readOnly = true)
   public UserResponseDto findById(UUID userId) {
      return userMapper.toResponse(findClinicUser(userId));
   }

   @Transactional
   public UserRoleResponseDto assignRole(UUID userId, UUID roleId) {
      User user = findClinicUser(userId);
      Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

      if (!user.getRoles().add(role)) {
         throw new BadRequestException("User already has this role");
      }

      return new UserRoleResponseDto(user.getId(), role.getId());
   }

   @Transactional
   public UserRoleResponseDto removeRole(UUID userId, UUID roleId) {
      User user = findClinicUser(userId);
      Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

      if (!user.getRoles().remove(role)) {
         throw new ResourceNotFoundException("User does not have this role");
      }

      return new UserRoleResponseDto(user.getId(), role.getId());
   }

   private User findClinicUser(UUID userId) {
      User user = userRepository.findByIdWithRoles(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      if (user.getClinic() == null) {
         throw new ResourceNotFoundException("Clinic user not found");
      }

      return user;
   }
}
