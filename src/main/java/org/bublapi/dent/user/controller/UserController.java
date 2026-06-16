package org.bublapi.dent.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.bublapi.dent.user.dto.*;
import org.bublapi.dent.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(summary = "Create new user", description = "Create a new user with provided information")
  @PostMapping
  public UserResponseDto create(@Valid @RequestBody CreateUserRequestDto request) {
    return userService.create(request);
  }

  @Operation(summary = "Update a user", description = "Update only provided fields of user")
  @PatchMapping("/{userId}")
  public UserResponseDto update(@PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRequestDto request) {
    return userService.update(userId, request);
  }

  @Operation(summary = "Assign a role", description = "Adds selected roles to user")
  @PostMapping("/{userId}/roles/{roleId}")
  public UserRoleResponseDto assignRole(@PathVariable UUID userId,
      @PathVariable UUID roleId) {
    return userService.assignRole(userId, roleId);
  }

  @Operation(summary = "Remove a role", description = "Removes selected roles from user")
  @DeleteMapping("/{userId}/roles/{roleId}")
  public UserRoleResponseDto removeRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
    return userService.removeRole(userId, roleId);
  }
}
