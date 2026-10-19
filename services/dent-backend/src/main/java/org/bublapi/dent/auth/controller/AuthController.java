package org.bublapi.dent.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.bublapi.dent.auth.dto.AuthTokens;
import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.auth.dto.LoginResponseDto;
import org.bublapi.dent.auth.dto.RegisterRequestDto;
import org.bublapi.dent.auth.entity.RefreshTokenType;
import org.bublapi.dent.auth.service.AuthService;
import org.bublapi.dent.auth.service.RefreshTokenCookieService;
import org.bublapi.dent.common.exception.InvalidRefreshTokenException;
import org.bublapi.dent.user.dto.CreateUserResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirement(name = "apiKey")
public class AuthController {
   private final AuthService authService;
   private final RefreshTokenCookieService cookieService;

   public AuthController(AuthService authService, RefreshTokenCookieService cookieService) {
      this.authService = authService;
      this.cookieService = cookieService;
   }

   @Operation(summary = "Login a user", description = "Login a user by email and response a token")
   @PostMapping("/login")
   public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request, HttpServletResponse response) {
      AuthTokens tokens = authService.login(request);

      cookieService.setUserCookie(response, tokens.refreshToken());

      return new LoginResponseDto(tokens.accessToken());
   }

   @Operation(summary = "Register a user", description = "Register a user")
   @PostMapping("/register")
   public CreateUserResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
      return authService.register(request);
   }

   @PostMapping("/refresh")
   public LoginResponseDto refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                   HttpServletResponse response) {
      if (refreshToken == null || refreshToken.isBlank()) {
         throw new InvalidRefreshTokenException();
      }

      AuthTokens tokens = authService.refresh(refreshToken, RefreshTokenType.USER);

      cookieService.setUserCookie(response, tokens.refreshToken());

      return new LoginResponseDto(tokens.accessToken());
   }

   @PostMapping("/logout")
   @ResponseStatus(HttpStatus.NO_CONTENT)
   public void logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                      HttpServletResponse response) {
      if (refreshToken != null && !refreshToken.isBlank()) {
         authService.logout(refreshToken, RefreshTokenType.USER);
      }

      cookieService.clearUserCookie(response);
   }
}
