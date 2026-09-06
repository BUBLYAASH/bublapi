package org.bublapi.dent.notification.service;

import org.bublapi.dent.common.exception.BadRequestException;
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
import org.bublapi.dent.notification.message.NotificationContent;
import org.bublapi.dent.notification.producer.NotificationProducer;
import org.bublapi.dent.notification.renderer.NotificationContentRenderer;
import org.bublapi.dent.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
   private final NotificationRepository notificationRepository;
   private final NotificationMapper notificationMapper;
   private final NotificationTransactionService transactionService;
   private final NotificationDispatcher notificationDispatcher;
   private final NotificationContentRenderer contentRenderer;
   private final NotificationProducer notificationProducer;

   public NotificationService(NotificationRepository notificationRepository, NotificationMapper notificationMapper,
                              NotificationTransactionService transactionService,
                              NotificationDispatcher notificationDispatcher,
                              NotificationContentRenderer contentRenderer, NotificationProducer notificationProducer) {
      this.notificationRepository = notificationRepository;
      this.notificationMapper = notificationMapper;
      this.transactionService = transactionService;
      this.notificationDispatcher = notificationDispatcher;
      this.contentRenderer = contentRenderer;
      this.notificationProducer = notificationProducer;
   }

   public void create(CreateNotificationCommand command) {
      NotificationContent content = contentRenderer.render(command.type(), command.data());

      send(command, NotificationChannel.IN_APP, content);
      send(command, NotificationChannel.EMAIL, content);
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

   public NotificationResponseDto findByIdForAdmin(UUID notificationId) {
      Notification notification = notificationRepository.findById(notificationId)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Notification not found"));

      return notificationMapper.toResponse(notification);
   }

   public UserNotificationResponseDto findByIdForPatient(UUID userId, UUID notificationId) {
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
   public void readAllForPatient(UUID userId) {
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

   @Transactional(readOnly = true)
   public void retry(UUID notificationId) {
      Notification notification = notificationRepository.findById(notificationId)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Notification not found"));

      if (notification.getStatus() != NotificationStatus.FAILED) {
         throw new BadRequestException("Only failed notifications can be retried");
      }

      if (notification.getData() == null) {
         throw new BadRequestException("Notification cannot be retried");
      }

      CreateNotificationCommand command = new CreateNotificationCommand(notification.getRequestId(),
                                                                        notification.getClinic().getId(),
                                                                        notification.getUser().getId(),
                                                                        notification.getAppointment() == null ? null : notification.getAppointment()
                                                                                                                                   .getId(),
                                                                        notification.getType(), notification.getData(),
                                                                        notification.getScheduledAt());

      notificationProducer.publish(command);
   }

   private void send(CreateNotificationCommand command, NotificationChannel channel, NotificationContent content) {
      Notification notification = transactionService.prepare(command, channel, content);

      if (notification.getStatus() == NotificationStatus.SENT) {
         return;
      }

      try {
         notificationDispatcher.dispatch(notification, command);

         transactionService.markAsSent(notification.getId());
      } catch (Exception e) {
         transactionService.markAsFailed(notification.getId(), e.getMessage());

         throw e;
      }
   }
}
