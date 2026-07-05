package org.bublapi.dent.user.controller.staff;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.auth.security.CustomUserDetails;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.dto.UserRoleResponseDto;
import org.bublapi.dent.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Users management for staff")
@RestController
@RequestMapping("/api/clinics/{clinicId}/users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("""
        hasAnyRole('ADMIN', 'OWNER', 'RECEPTIONIST')
        and @clinicSecurity.hasAccess(authentication, #clinicId)
        """)
public class StaffUserController {

   private final UserService userService;

   public StaffUserController(UserService userService) {
      this.userService = userService;
   }

   @Operation(summary = "Assign a role", description = "Adds selected role to user")
   @PostMapping("/{userId}/roles/{roleId}")
   public UserRoleResponseDto assignRole(@PathVariable UUID clinicId, @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID userId, @PathVariable UUID roleId) {
      return userService.assignRole(clinicId, userDetails.getId(), userId, roleId);
   }

   @Operation(summary = "Remove a role", description = "Removes selected role¬ from user")
   @DeleteMapping("/{userId}/roles/{roleId}")
   public UserRoleResponseDto removeRole(@PathVariable UUID clinicId, @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID userId, @PathVariable UUID roleId) {
      return userService.removeRole(clinicId, userDetails.getId(), userId, roleId);
   }

   @Operation(summary = "Deactivate user's account", description = "Deactivates the user account by user ID")
   @PatchMapping("/{userId}/deactivation")
   public UserResponseDto deactivate(@PathVariable UUID clinicId, @PathVariable UUID userId) {
      return userService.deactivate(clinicId, userId);
   }

   @Operation(summary = "Activate user's account", description = "Activates the user account by user ID")
   @PatchMapping("/{userId}/activation")
   public UserResponseDto activate(@PathVariable UUID clinicId, @PathVariable UUID userId) {
      return userService.activate(clinicId, userId);
   }

   @Operation(summary = "Get all enabled users in clinic", description = "Get all enabled users in this clinic")
   @GetMapping
   public List<UserResponseDto> findAll(@PathVariable UUID clinicId) {
      return userService.findAll(clinicId);
   }

   @Operation(summary = "Get one user by ID", description = "Get one user by ID")
   @GetMapping("/{userId}")
   public UserResponseDto findById(@PathVariable UUID clinicId, @PathVariable UUID userId) {
      return userService.findById(clinicId, userId);
   }
}
