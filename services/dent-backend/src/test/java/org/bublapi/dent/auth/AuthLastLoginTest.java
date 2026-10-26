package org.bublapi.dent.auth;

import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.auth.entity.RefreshTokenType;
import org.bublapi.dent.auth.service.AuthService;
import org.bublapi.dent.auth.service.JwtService;
import org.bublapi.dent.auth.service.RefreshTokenService;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.logging.security.SecurityLogService;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.bublapi.dent.user.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthLastLoginTest {
   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void tokenCreationFailureCannotRecordLogin(boolean admin) {
      var users = mock(UserRepository.class);
      var passwords = mock(PasswordEncoder.class);
      var jwt = mock(JwtService.class);
      var refresh = mock(RefreshTokenService.class);
      var auth = new AuthService(users, passwords, jwt, mock(UserService.class), mock(SecurityLogService.class),
                                 refresh);
      var clinic = new Clinic();
      clinic.setId(UUID.randomUUID());
      var user = new User();
      user.setId(UUID.randomUUID());
      user.setPasswordHash("hash");
      when(users.findAdminByEmailWithRoles("user@test.com")).thenReturn(Optional.of(user));
      when(users.findByEmailIgnoreCaseAndClinic_Id("user@test.com", clinic.getId())).thenReturn(Optional.of(user));
      when(passwords.matches("password", "hash")).thenReturn(true);
      when(jwt.generateToken(user.getId())).thenReturn("access");
      when(refresh.create(user, admin ? RefreshTokenType.ADMIN : RefreshTokenType.USER)).thenThrow(
              new IllegalStateException("storage failure"));
      ClinicContext.set(clinic);
      try {
         assertThatThrownBy(() -> {
            if (admin) auth.loginAdmin(new LoginRequestDto("user@test.com", "password"));
            else auth.login(new LoginRequestDto("user@test.com", "password"));
         }).isInstanceOf(IllegalStateException.class);
         verify(users, never()).recordLogin(any(), any());
      } finally {
         ClinicContext.clear();
      }
   }
}
