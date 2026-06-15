package org.bublapi.dent.user.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.UpdateUserRequestDto;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public UserResponseDto create(@Valid @RequestBody CreateUserRequestDto request) {
    return userService.create(request);
  }

  @PatchMapping("/{id}")
  public UserResponseDto update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequestDto request) {
    return userService.update(id, request);
  }
}
