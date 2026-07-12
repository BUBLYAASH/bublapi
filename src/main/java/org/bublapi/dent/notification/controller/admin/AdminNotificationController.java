package org.bublapi.dent.notification.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.notification.dto.NotificationResponseDto;
import org.bublapi.dent.notification.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notifications for Admin")
@RestController
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
   private final NotificationService notificationService;

   public AdminNotificationController(NotificationService notificationService) {
      this.notificationService = notificationService;
   }

   @Operation(summary = "Show all notifications", description = "Shows all notifications")
   @GetMapping
   public List<NotificationResponseDto> findAll() {
      return notificationService.findAllForAdmin();
   }

   @Operation(summary = "Show a notification", description = "Shows a notification by provided ID")
   @GetMapping("/{notificationId}")
   public NotificationResponseDto findById(@PathVariable UUID notificationId) {
      return notificationService.findByIdForAdmin(notificationId);
   }
}
