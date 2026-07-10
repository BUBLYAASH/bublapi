package org.bublapi.dent.auth.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.auth.dto.LoginResponseDto;
import org.bublapi.dent.auth.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "To login ADMINs")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
   private final AuthService authService;

   public AdminAuthController(AuthService authService) {
      this.authService = authService;
   }

   @Operation(summary = "Login an admin", description = "Logins an admin that has not clinic id")
   @PostMapping("/login")
   public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
      return authService.loginAdmin(request);
   }
}
