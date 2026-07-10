package org.bublapi.dent.notification.controller.patient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.auth.security.CustomUserDetails;
import org.bublapi.dent.notification.dto.UnreadNotificationsCountResponseDto;
import org.bublapi.dent.notification.dto.UserNotificationResponseDto;
import org.bublapi.dent.notification.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notifications for Patients")
@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiKey")
@PreAuthorize("hasRole('PATIENT')")
public class PatientNotificationController {
   private final NotificationService notificationService;

   public PatientNotificationController(NotificationService notificationService) {
      this.notificationService = notificationService;
   }

   @Operation(summary = "Show all patient's notifications", description = "Shows all not deleted patient's notifications")
   @GetMapping
   public List<UserNotificationResponseDto> findAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
      return notificationService.findAllSent(userDetails.getId());
   }

   @Operation(summary = "Show detailed information about one notification", description = "Shows detailed information about one notification")
   @GetMapping("/{notificationId}")
   public UserNotificationResponseDto findById(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID notificationId) {
      return notificationService.findById(userDetails.getId(), notificationId);
   }

   @Operation(summary = "Unread notifications", description = "Count unread notifications")
   @GetMapping("/unread-count")
   public UnreadNotificationsCountResponseDto unreadNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
      return notificationService.unreadCount(userDetails.getId());
   }

   @Operation(summary = "Read a notification", description = "Read a notification")
   @PatchMapping("/{notificationId}/read")
   public void read(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID notificationId) {
      notificationService.readNotification(userDetails.getId(), notificationId);
   }

   @Operation(summary = "Read all notifications", description = "Read all notifications")
   @PatchMapping("/read-all")
   public void readAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
      notificationService.readAll(userDetails.getId());
   }

   @Operation(summary = "Delete a notification", description = "Soft deletes a notification")
   @DeleteMapping("/{notificationId}")
   public void delete(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID notificationId) {
      notificationService.deleteNotification(userDetails.getId(), notificationId);
   }
}
