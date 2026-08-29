package org.bublapi.dent.user.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.dto.UserRoleResponseDto;
import org.bublapi.dent.user.service.AdminUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Users management for admin")
@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

   private final AdminUserService adminUserService;

   public AdminUserController(AdminUserService adminUserService) {
      this.adminUserService = adminUserService;
   }

   @Operation(summary = "Get clinic users", description = "Returns users from all managed clinics")
   @GetMapping
   public List<UserResponseDto> findAll() {
      return adminUserService.findAll();
   }

   @Operation(summary = "Get clinic user by ID")
   @GetMapping("/{userId}")
   public UserResponseDto findById(@PathVariable UUID userId) {
      return adminUserService.findById(userId);
   }

   @Operation(summary = "Assign role to clinic user")
   @PostMapping("/{userId}/roles/{roleId}")
   public UserRoleResponseDto assignRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
      return adminUserService.assignRole(userId, roleId);
   }

   @Operation(summary = "Remove role from clinic user")
   @DeleteMapping("/{userId}/roles/{roleId}")
   public UserRoleResponseDto removeRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
      return adminUserService.removeRole(userId, roleId);
   }
}
