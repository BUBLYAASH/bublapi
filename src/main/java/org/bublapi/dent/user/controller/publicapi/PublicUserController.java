package org.bublapi.dent.user.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.bublapi.dent.user.service.UserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Public user registration")
@RestController
@RequestMapping("/api/public/clinics/{clinicId}/users")
public class PublicUserController {

   private final UserService userService;

   public PublicUserController(UserService userService) {
      this.userService = userService;
   }

   @Operation(summary = "Create new user", description = "Creates a new user with provided information")
   @PostMapping
   public UserResponseDto createUser(@PathVariable UUID clinicId, @Valid @RequestBody CreateUserRequestDto request) {
      return userService.create(clinicId, request);
   }
}
