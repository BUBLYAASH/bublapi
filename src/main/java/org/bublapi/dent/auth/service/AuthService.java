package org.bublapi.dent.auth.service;

import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.auth.dto.LoginResponseDto;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
   private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder;
   private final JwtService jwtService;

   public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
      this.userRepository = userRepository;
      this.passwordEncoder = passwordEncoder;
      this.jwtService = jwtService;
   }

   public LoginResponseDto login(LoginRequestDto request) {
      User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
      // TODO: after adding clinic API Key authentication
      //  find user by its email and clinic_id

      boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

      if (!passwordMatches) {
         throw new BadRequestException("Invalid email or password");
      }

      if (!user.getEnabled()) {
         throw new BadRequestException("User is disabled");
      }

      String token = jwtService.generateToken(user.getEmail());

      return new LoginResponseDto(token);
   }
}
