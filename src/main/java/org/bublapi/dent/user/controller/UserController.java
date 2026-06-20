package org.bublapi.dent.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.UpdateUserRequestDto;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.dto.UserRoleResponseDto;
import org.bublapi.dent.user.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/clinics/{clinicId}/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(summary = "Create new user", description = "Create a new user with provided information")
  @PostMapping
  public UserResponseDto createUser(@PathVariable UUID clinicId,
      @Valid @RequestBody CreateUserRequestDto request) {
    return userService.create(clinicId, request);
  }

  @Operation(summary = "Update a user", description = "Update only provided fields of user")
  @PatchMapping("/{userId}")
  public UserResponseDto updateUser(@PathVariable UUID clinicId, @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRequestDto request) {
    return userService.update(clinicId, userId, request);
  }

  @Operation(summary = "Assign a role", description = "Adds selected role to user")
  @PostMapping("/{userId}/roles/{roleId}")
  public UserRoleResponseDto assignRole(@PathVariable UUID clinicId, @PathVariable UUID userId,
      @PathVariable UUID roleId) {
    return userService.assignRole(clinicId, userId, roleId);
  }

  @Operation(summary = "Remove a role", description = "Removes selected role¬ from user")
  @DeleteMapping("/{userId}/roles/{roleId}")
  public UserRoleResponseDto removeRole(@PathVariable UUID clinicId, @PathVariable UUID userId,
      @PathVariable UUID roleId) {
    return userService.removeRole(clinicId, userId, roleId);
  }

  @Operation(summary = "Deactivate a user", description = "Deactivates user by ID")
  @PatchMapping("/{userId}/deactivation")
  public UserResponseDto deactivate(@PathVariable UUID clinicId, @PathVariable UUID userId) {
    return userService.deactivate(clinicId, userId);
  }

  @Operation(summary = "Activate a user", description = "Activates user by ID")
  @PatchMapping("/{userId}/activation")
  public UserResponseDto activate(@PathVariable UUID clinicId, @PathVariable UUID userId) {
    return userService.activate(clinicId, userId);
  }

  @Operation(summary = "Get all enabled users in clinic", description = "Get all enabled users in this clinic")
  @GetMapping
  public List<UserResponseDto> findAll(@PathVariable UUID clinicId) {
    return userService.findAll(clinicId);
  }
}
