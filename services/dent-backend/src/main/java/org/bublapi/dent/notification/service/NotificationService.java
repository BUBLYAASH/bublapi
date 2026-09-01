package org.bublapi.dent.notification.service;

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

   public NotificationService(NotificationRepository notificationRepository, NotificationMapper notificationMapper,
                              NotificationTransactionService transactionService,
                              NotificationDispatcher notificationDispatcher) {
      this.notificationRepository = notificationRepository;
      this.notificationMapper = notificationMapper;
      this.transactionService = transactionService;
      this.notificationDispatcher = notificationDispatcher;
   }

   public void create(CreateNotificationCommand command) {
      Notification notification = transactionService.prepare(command);

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

   // TODO:
   //  - POST /api/admin/notifications/{notificationId}/retry
}
