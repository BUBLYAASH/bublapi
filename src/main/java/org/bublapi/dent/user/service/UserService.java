package org.bublapi.dent.user.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationType;
import org.bublapi.dent.notification.publisher.NotificationPublisher;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.role.repository.RoleRepository;
import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.CreateUserResponseDto;
import org.bublapi.dent.user.dto.PatientCardLinkStatus;
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
   private final RoleRepository roleRepository;
   private final PatientRepository patientRepository;
   private final UserMapper userMapper;
   private final PasswordEncoder passwordEncoder;
   private final NotificationPublisher notificationPublisher;

   public UserService(UserRepository userRepository, RoleRepository roleRepository, PatientRepository patientRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, NotificationPublisher notificationPublisher) {
      this.userRepository = userRepository;
      this.roleRepository = roleRepository;
      this.patientRepository = patientRepository;
      this.userMapper = userMapper;
      this.passwordEncoder = passwordEncoder;
      this.notificationPublisher = notificationPublisher;
   }

   private void publishUserNotifications(User user, NotificationType type, String title, String message) {
      String userEmail = user.getEmail();

      notificationPublisher.publishAfterCommit(
              new CreateNotificationCommand(user.getClinic().getId(), user.getId(), null, type,
                                            NotificationChannel.IN_APP, userEmail, title, message, null));

      if (userEmail != null && !userEmail.isBlank()) {
         notificationPublisher.publishAfterCommit(
                 new CreateNotificationCommand(user.getClinic().getId(), user.getId(), null, type,
                                               NotificationChannel.EMAIL, userEmail, title, message, null));
      }
   }

   @Transactional
   public CreateUserResponseDto create(CreateUserRequestDto request) {
      Clinic clinic = ClinicContext.get();

      Role patientRole = roleRepository.findByName(RoleName.PATIENT)
                                       .orElseThrow(() -> new ResourceNotFoundException("PATIENT role not found"));

      User user = userMapper.toEntity(request);
      user.setClinic(clinic);
      user.setPasswordHash(passwordEncoder.encode(request.password()));
      user.setRoles(Set.of(patientRole));

      User saved = userRepository.save(user);

      publishUserNotifications(saved, NotificationType.USER_REGISTERED, "Вы успешно зарегистрировались",
                               "Ваш аккаунт успешно создан!");

      PatientCardLinkStatus cardStatus = PatientCardLinkStatus.NOT_FOUND;
      String cardMessage = "Карточка пациента не найдена.";

      var patientOptional = patientRepository.findByEmailOrPhone(request.email(), request.phone());

      if (patientOptional.isPresent()) {
         var patient = patientOptional.get();

         if (patient.getUser() == null) {
            patient.setUser(saved);

            cardStatus = PatientCardLinkStatus.LINKED;
            cardMessage = "Мы обнаружили карточку пациента по некоторым Вашим данным и успешно привязали ее к аккаунту!";

            publishUserNotifications(saved, NotificationType.PATIENT_CARD_LINKED, "Мы обнаружили карточку пациента",
                                     "Мы обнаружили карточку пациента по некоторым Вашим данным и уже привязали ее к Вашему аккаунту!");
         } else {
            cardStatus = PatientCardLinkStatus.ALREADY_LINKED_TO_ANOTHER_USER;
            cardMessage = "Мы обнаружили карточку пациента по некоторым Вашим данным, но по каким-то причинам она уже привязана к другому аккаунту. Для решения этой проблемы обратитесь к администратору клиники.";

            publishUserNotifications(saved, NotificationType.PATIENT_CARD_IS_BUSY, "Карточка пациента занята...",
                                     "Мы обнаружили карточку пациента по некоторым Вашим данным, но по каким-то причинам она уже привязана к другому аккаунту. Для решения этой проблемы обратитесь к администратору клиники.");
         }
      }

      return new CreateUserResponseDto(userMapper.toResponse(saved), cardStatus, cardMessage);
   }

   @Transactional
   public UserResponseDto update(UUID userId, UpdateUserRequestDto request) {
      User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

      userMapper.updateEntity(request, user);

      if (request.password() != null && !request.password().isBlank()) {
         user.setPasswordHash(passwordEncoder.encode(request.password()));
      }

      return userMapper.toResponse(user);
   }

   @Transactional
   public UserRoleResponseDto assignRole(UUID actorUserId, UUID targetUserId, UUID roleId) {
      User actorUser = userRepository.findById(actorUserId)
                                     .orElseThrow(() -> new ResourceNotFoundException("Actor user not found"));

      User targetUser = userRepository.findById(targetUserId)
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
   public UserRoleResponseDto removeRole(UUID actorUserId, UUID targetUserId, UUID roleId) {
      User actorUser = userRepository.findById(actorUserId)
                                     .orElseThrow(() -> new ResourceNotFoundException("Actor user not found"));

      User targetUser = userRepository.findById(targetUserId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

      Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

      RoleName roleName = role.getName();

      boolean allowed = actorUser.getRoles().stream().anyMatch(r -> r.getName().canRemove(roleName));

      if (!allowed) {
         throw new AccessDeniedException("You cannot remove this role");
      }

      if (role.getName().equals(RoleName.PATIENT) && actorUser.getRoles()
                                                              .stream()
                                                              .noneMatch(r -> r.getName() == RoleName.ADMIN)) {
         throw new AccessDeniedException("You cannot remove PATIENT role");
      }

      boolean removed = targetUser.getRoles().remove(role);

      if (!removed) {
         throw new ResourceNotFoundException("User does not have this role");
      }

      return new UserRoleResponseDto(targetUser.getId(), role.getId());
   }

   @Transactional
   public UserResponseDto deactivate(UUID userId) {
      User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

      user.setEnabled(false);
      user.setDisabledByClinic(false);

      publishUserNotifications(user, NotificationType.USER_DEACTIVATED, "Ваш аккаунт отключен",
                               "Ваш аккаунт успешно отключен");

      return userMapper.toResponse(user);
   }

   @Transactional
   public UserResponseDto activate(UUID userId) {
      User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

      user.setEnabled(true);
      user.setDisabledByClinic(false);

      publishUserNotifications(user, NotificationType.USER_ACTIVATED, "Ваш аккаунт активирован",
                               "Ваш аккаунт снова активирован");

      return userMapper.toResponse(user);
   }

   public List<UserResponseDto> findAll() {
      return userRepository.findAll().stream().map(userMapper::toResponse).toList();
   }

   public UserResponseDto findById(UUID userId) {
      User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found or unavailable"));

      return userMapper.toResponse(user);
   }
}
