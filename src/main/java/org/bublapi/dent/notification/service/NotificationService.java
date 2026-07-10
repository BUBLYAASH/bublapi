package org.bublapi.dent.notification.service;

import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.repository.AppointmentRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.notification.command.CreateNotificationCommand;
import org.bublapi.dent.notification.dispatcher.NotificationDispatcher;
import org.bublapi.dent.notification.dto.NotificationResponseDto;
import org.bublapi.dent.notification.dto.UnreadNotificationsCountResponseDto;
import org.bublapi.dent.notification.dto.UserNotificationResponseDto;
import org.bublapi.dent.notification.entity.Notification;
import org.bublapi.dent.notification.entity.NotificationChannel;
import org.bublapi.dent.notification.entity.NotificationStatus;
import org.bublapi.dent.notification.mapper.NotificationMapper;
import org.bublapi.dent.notification.repository.NotificationRepository;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
   private final NotificationRepository notificationRepository;
   private final NotificationMapper notificationMapper;
   private final AppointmentRepository appointmentRepository;
   private final ClinicRepository clinicRepository;
   private final UserRepository userRepository;
   private final NotificationDispatcher notificationDispatcher;

   public NotificationService(NotificationRepository notificationRepository, NotificationMapper notificationMapper, AppointmentRepository appointmentRepository, ClinicRepository clinicRepository, UserRepository userRepository, NotificationDispatcher notificationDispatcher) {
      this.notificationRepository = notificationRepository;
      this.notificationMapper = notificationMapper;
      this.appointmentRepository = appointmentRepository;
      this.clinicRepository = clinicRepository;
      this.userRepository = userRepository;
      this.notificationDispatcher = notificationDispatcher;
   }

   @Transactional
   public Notification create(CreateNotificationCommand command) {
      Clinic clinic = clinicRepository.findById(command.clinicId())
                                      .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

      Notification notification = new Notification();

      if (command.appointmentId() != null) {
         Appointment appointment = appointmentRepository.findById(command.appointmentId())
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Appointment not found"));

         notification.setAppointment(appointment);
      }

      if (command.userId() != null) {
         User user = userRepository.findById(command.userId())
                                   .orElseThrow(() -> new ResourceNotFoundException("User not found"));

         notification.setUser(user);
      }

      notification.setClinic(clinic);
      notification.setType(command.type());
      notification.setChannel(command.channel());
      notification.setTitle(command.title());
      notification.setScheduledAt(command.scheduledAt());
      notification.setMessage(command.message());
      notification.setStatus(NotificationStatus.PENDING);

      Notification saved = notificationRepository.save(notification);

      try {
         notificationDispatcher.dispatch(saved, command);

         saved.setStatus(NotificationStatus.SENT);
         saved.setSentAt(LocalDateTime.now());
      } catch (Exception e) {
         saved.setStatus(NotificationStatus.FAILED);
         throw e;
      }

      return saved;
   }

   @Transactional
   public void markAsSent(UUID notificationId) {
      Notification notification = notificationRepository.findById(notificationId)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Notification not found"));

      notification.setStatus(NotificationStatus.SENT);
      notification.setSentAt(LocalDateTime.now());
   }

   @Transactional
   public void markAsFailed(UUID notificationId) {
      Notification notification = notificationRepository.findById(notificationId)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Notification not found"));

      notification.setStatus(NotificationStatus.FAILED);
   }

   public List<UserNotificationResponseDto> findAllSent(UUID userId) {
      return notificationRepository.findAllByUser_IdAndChannelAndStatusAndDeletedFalseOrderBySentAtDesc(userId,
                                                                                                        NotificationChannel.IN_APP,
                                                                                                        NotificationStatus.SENT)
                                   .stream()
                                   .map(notificationMapper::toUserResponse)
                                   .toList();
   }

   public List<NotificationResponseDto> findAllForAdmin() {
      return notificationRepository.findAllByOrderByCreatedAtDesc()
                                   .stream()
                                   .map(notificationMapper::toResponse)
                                   .toList();
   }

   public UserNotificationResponseDto findById(UUID userId, UUID notificationId) {
      Notification notification = notificationRepository.findByIdAndUser_IdAndChannelAndStatusAndDeletedFalse(
                                                                notificationId, userId, NotificationChannel.IN_APP, NotificationStatus.SENT)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Notification not found"));

      return notificationMapper.toUserResponse(notification);
   }

   public UnreadNotificationsCountResponseDto unreadCount(UUID userId) {
      return new UnreadNotificationsCountResponseDto(
              notificationRepository.countByUser_IdAndChannelAndStatusAndReadFalseAndDeletedFalse(userId,
                                                                                                  NotificationChannel.IN_APP,
                                                                                                  NotificationStatus.SENT));
   }

   @Transactional
   public void readNotification(UUID userId, UUID notificationId) {
      Notification notification = notificationRepository.findByIdAndUser_IdAndChannelAndStatusAndDeletedFalse(
                                                                notificationId, userId, NotificationChannel.IN_APP, NotificationStatus.SENT)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Notification not found"));

      if (!notification.isRead()) {
         notification.setRead(true);
         notification.setReadAt(LocalDateTime.now());
      }
   }

   @Transactional
   public void readAll(UUID userId) {
      List<Notification> notifications = notificationRepository.findAllByUser_IdAndChannelAndStatusAndReadFalseAndDeletedFalse(
              userId, NotificationChannel.IN_APP, NotificationStatus.SENT);

      LocalDateTime now = LocalDateTime.now();

      notifications.forEach(notification -> {
         notification.setRead(true);
         notification.setReadAt(now);
      });
   }

   @Transactional
   public void deleteNotification(UUID userId, UUID notificationId) {
      Notification notification = notificationRepository.findByIdAndUser_IdAndChannelAndStatusAndDeletedFalse(
                                                                notificationId, userId, NotificationChannel.IN_APP, NotificationStatus.SENT)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Notification not found"));

      if (!notification.isDeleted()) {
         notification.setDeleted(true);
         notification.setDeletedAt(LocalDateTime.now());
      }
   }

   // TODO:
   //  - GET /api/admin/notifications/{notificationId}
   //  - POST /api/admin/notifications/{notificationId}/retry
   //  - implement proper FAILED status persistence before retry
}
