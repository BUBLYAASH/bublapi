package org.bublapi.dent.auth.service;

import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.auth.dto.LoginResponseDto;
import org.bublapi.dent.auth.dto.RegisterRequestDto;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
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

   public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, UserService userService) {
      this.userRepository = userRepository;
      this.passwordEncoder = passwordEncoder;
      this.jwtService = jwtService;
      this.userService = userService;
   }

   public LoginResponseDto login(LoginRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();
      String email = request.email().trim().toLowerCase(Locale.ROOT);

      User user = userRepository.findByEmailIgnoreCaseAndClinic_Id(email, clinicId)
                                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

      boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

      if (!passwordMatches) {
         throw new BadRequestException("Invalid email or password");
      }

      if (!user.getEnabled()) {
         throw new BadRequestException("User is disabled");
      }

      String token = jwtService.generateToken(user.getId());

      return new LoginResponseDto(token);
   }

   public LoginResponseDto loginAdmin(LoginRequestDto request) {
      String email = request.email().trim().toLowerCase(Locale.ROOT);

      User user = userRepository.findAdminByEmailWithRoles(email)
                                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

      boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

      if (!passwordMatches) {
         throw new BadRequestException("Invalid email or password");
      }

      if (!user.getEnabled()) {
         throw new BadRequestException("User is disabled");
      }

      String token = jwtService.generateToken(user.getId());

      return new LoginResponseDto(token);
   }

   public CreateUserResponseDto register(RegisterRequestDto request) {
      return userService.create(
              new CreateUserRequestDto(request.email(), request.phone(), request.firstName(), request.lastName(),
                                       request.middleName(), request.password()));
   }
}
