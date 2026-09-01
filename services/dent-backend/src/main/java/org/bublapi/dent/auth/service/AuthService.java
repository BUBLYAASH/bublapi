package org.bublapi.dent.auth.service;

import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.auth.dto.LoginResponseDto;
import org.bublapi.dent.auth.dto.RegisterRequestDto;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.logging.SecurityLogService;
import org.bublapi.dent.user.dto.CreateUserRequestDto;
import org.bublapi.dent.user.dto.CreateUserResponseDto;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.bublapi.dent.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
   private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder;
   private final JwtService jwtService;
   private final UserService userService;
   private final SecurityLogService securityLogService;

   public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                      UserService userService, SecurityLogService securityLogService) {
      this.userRepository = userRepository;
      this.passwordEncoder = passwordEncoder;
      this.jwtService = jwtService;
      this.userService = userService;
      this.securityLogService = securityLogService;
   }

   public LoginResponseDto login(LoginRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();
      String email = request.email().trim().toLowerCase(Locale.ROOT);

      User user = userRepository.findByEmailIgnoreCaseAndClinic_Id(email, clinicId).orElseThrow(() -> {
         securityLogService.loginFailed("INVALID_CREDENTIALS");
         return new BadRequestException("Invalid email or password");
      });

      boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

      if (!passwordMatches) {
         securityLogService.loginFailed("INVALID_CREDENTIALS");

         throw new BadRequestException("Invalid email or password");
      }

      if (!user.isEnabled()) {
         securityLogService.loginFailed("USER_DISABLED");

         throw new BadRequestException("User is disabled");
      }

      securityLogService.loginSuccess(user.getId(), clinicId);

      String token = jwtService.generateToken(user.getId());

      return new LoginResponseDto(token);
   }

   public LoginResponseDto loginAdmin(LoginRequestDto request) {
      String email = request.email().trim().toLowerCase(Locale.ROOT);

      User user = userRepository.findAdminByEmailWithRoles(email).orElseThrow(() -> {
         securityLogService.adminLoginFailed("INVALID_CREDENTIALS");
         return new BadRequestException("Invalid email or password");
      });

      boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

      if (!passwordMatches) {
         securityLogService.adminLoginFailed("INVALID_CREDENTIALS");

         throw new BadRequestException("Invalid email or password");
      }

      if (!user.isEnabled()) {
         securityLogService.adminLoginFailed("USER_DISABLED");

         throw new BadRequestException("User is disabled");
      }

      securityLogService.adminLoginSuccess(user.getId());

      String token = jwtService.generateToken(user.getId());

      return new LoginResponseDto(token);
   }

   public CreateUserResponseDto register(RegisterRequestDto request) {
      return userService.create(
              new CreateUserRequestDto(request.email(), request.phone(), request.firstName(), request.lastName(),
                                       request.middleName(), request.password()));
   }
}
