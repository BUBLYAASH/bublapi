package org.bublapi.dent.user.service;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.logging.AdministrativeAuditService;
import org.bublapi.dent.logging.SecurityLogService;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.command.UserNotificationData;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
   private final SecurityLogService securityLogService;
   private final AdministrativeAuditService administrativeAuditService;

   public UserService(UserRepository userRepository, RoleRepository roleRepository, PatientRepository patientRepository,
                      UserMapper userMapper, PasswordEncoder passwordEncoder,
                      NotificationPublisher notificationPublisher, SecurityLogService securityLogService,
                      AdministrativeAuditService administrativeAuditService) {
      this.userRepository = userRepository;
      this.roleRepository = roleRepository;
      this.patientRepository = patientRepository;
      this.userMapper = userMapper;
      this.passwordEncoder = passwordEncoder;
      this.notificationPublisher = notificationPublisher;
      this.securityLogService = securityLogService;
      this.administrativeAuditService = administrativeAuditService;
   }

   @Transactional
   public CreateUserResponseDto create(CreateUserRequestDto request) {
      Clinic clinic = ClinicContext.get();

      String email = request.email().trim().toLowerCase(Locale.ROOT);
      String phone = request.phone().trim();

      Role patientRole = roleRepository.findByName(RoleName.PATIENT)
                                       .orElseThrow(() -> new ResourceNotFoundException("PATIENT role not found"));

      User user = userMapper.toEntity(request);
      user.setClinic(clinic);
      user.setEmail(email);
      user.setPhone(phone);
      user.setPasswordHash(passwordEncoder.encode(request.password()));
      user.setRoles(Set.of(patientRole));

      User saved = userRepository.save(user);

      publishUserNotification(saved, NotificationType.USER_REGISTERED);

      PatientCardLinkStatus cardStatus = PatientCardLinkStatus.NOT_FOUND;
      String cardMessage = "Карточка пациента не найдена.";

      var patientOptional = patientRepository.findByClinic_IdAndEmailIgnoreCaseOrPhone(clinic.getId(), email, phone);

      if (patientOptional.isPresent()) {
         var patient = patientOptional.get();

         if (patient.getUser() == null) {
            patient.setUser(saved);

            cardStatus = PatientCardLinkStatus.LINKED;
            cardMessage = "Мы обнаружили карточку пациента по некоторым Вашим данным и успешно привязали ее к аккаунту!";

            publishUserNotification(saved, NotificationType.PATIENT_CARD_LINKED);
         } else {
            cardStatus = PatientCardLinkStatus.ALREADY_LINKED_TO_ANOTHER_USER;
            cardMessage = "Мы обнаружили карточку пациента по некоторым Вашим данным, но по каким-то причинам она уже привязана к другому аккаунту. Для решения этой проблемы обратитесь к администратору клиники.";

            publishUserNotification(saved, NotificationType.PATIENT_CARD_IS_BUSY);
         }
      }

      return new CreateUserResponseDto(userMapper.toResponse(saved), cardStatus, cardMessage);
   }

   @Transactional
   public UserResponseDto update(UUID userId, UpdateUserRequestDto request) {
      String email = request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT);
      String phone = request.phone() == null ? null : request.phone().trim();

      UUID clinicId = ClinicContext.getClinicId();

      User user = userRepository.findByIdAndClinic_Id(userId, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      userMapper.updateEntity(request, user);

      if (request.password() != null && !request.password().isBlank()) {
         user.setPasswordHash(passwordEncoder.encode(request.password()));
      }

      if (email != null && !email.isBlank()) {
         user.setEmail(email);
      }

      if (phone != null && !phone.isBlank()) {
         user.setPhone(phone);
      }

      return userMapper.toResponse(user);
   }

   @Transactional
   public UserRoleResponseDto assignRole(UUID actorUserId, UUID targetUserId, UUID roleId) {
      UUID clinicId = ClinicContext.getClinicId();

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

      administrativeAuditService.roleGranted(targetUser.getId(), clinicId, roleName.name());

      return new UserRoleResponseDto(targetUser.getId(), role.getId());
   }

   @Transactional
   public UserRoleResponseDto removeRole(UUID actorUserId, UUID targetUserId, UUID roleId) {
      UUID clinicId = ClinicContext.getClinicId();

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

      if (role.getName().equals(RoleName.PATIENT) && actorUser.getRoles()
                                                              .stream()
                                                              .noneMatch(r -> r.getName() == RoleName.ADMIN)) {
         throw new AccessDeniedException("You cannot remove PATIENT role");
      }

      boolean removed = targetUser.getRoles().remove(role);

      if (!removed) {
         throw new ResourceNotFoundException("User does not have this role");
      }

      administrativeAuditService.roleRevoked(targetUser.getId(), clinicId, roleName.name());

      return new UserRoleResponseDto(targetUser.getId(), role.getId());
   }

   @Transactional
   public UserResponseDto deactivate(UUID userId) {
      UUID clinicId = ClinicContext.getClinicId();

      User user = userRepository.findByIdAndClinic_Id(userId, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      if (!user.isEnabled()) {
         throw new BadRequestException("User is already deactivated");
      }

      user.setEnabled(false);
      user.setDisabledByClinic(false);

      publishUserNotification(user, NotificationType.USER_DEACTIVATED);

      securityLogService.userDeactivated(user.getId(), clinicId);

      administrativeAuditService.userDeactivated(user.getId(), clinicId);

      return userMapper.toResponse(user);
   }

   @Transactional
   public UserResponseDto activate(UUID userId) {
      UUID clinicId = ClinicContext.getClinicId();

      User user = userRepository.findByIdAndClinic_Id(userId, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      if (user.isEnabled()) {
         throw new BadRequestException("User is already active");
      }

      user.setEnabled(true);
      user.setDisabledByClinic(false);

      publishUserNotification(user, NotificationType.USER_ACTIVATED);

      securityLogService.userActivated(user.getId(), clinicId);

      administrativeAuditService.userActivated(user.getId(), clinicId);

      return userMapper.toResponse(user);
   }

   public List<UserResponseDto> findAll() {
      UUID clinicId = ClinicContext.getClinicId();

      return userRepository.findAllByClinic_Id(clinicId).stream().map(userMapper::toResponse).toList();
   }

   public UserResponseDto findById(UUID userId) {
      UUID clinicId = ClinicContext.getClinicId();

      User user = userRepository.findByIdAndClinic_Id(userId, clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found or unavailable"));

      return userMapper.toResponse(user);
   }

   private void publishUserNotification(User user, NotificationType type) {
      notificationPublisher.publishAfterCommit(
              new CreateNotificationCommand(user.getClinic().getId(), user.getId(), null, type,
                                            new UserNotificationData(user.getClinic().getTitle(), user.getFirstName()),
                                            LocalDateTime.now()));
   }
}
