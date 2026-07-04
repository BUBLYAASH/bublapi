package org.bublapi.dent.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.auth.dto.LoginResponseDto;
import org.bublapi.dent.auth.dto.RegisterRequestDto;
import org.bublapi.dent.auth.service.AuthService;
import org.bublapi.dent.user.dto.UserResponseDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth/{clinicId}")
public class AuthController {
   private final AuthService authService;

   public AuthController(AuthService authService) {
      this.authService = authService;
   }

   @Operation(summary = "Login a user", description = "Login a user by email and response a token")
   @PostMapping("/login")
   public LoginResponseDto login(@PathVariable UUID clinicId, @Valid @RequestBody LoginRequestDto request) {
      return authService.login(clinicId, request);
   }

   @Operation(summary = "Register a user", description = "Register a user")
   @PostMapping("/register")
   public UserResponseDto register(@PathVariable UUID clinicId, @Valid @RequestBody RegisterRequestDto request) {
      return authService.register(clinicId, request);
   }
}
