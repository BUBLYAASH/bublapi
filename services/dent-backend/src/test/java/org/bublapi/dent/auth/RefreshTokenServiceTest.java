package org.bublapi.dent.auth;

import org.bublapi.dent.auth.entity.RefreshToken;
import org.bublapi.dent.auth.entity.RefreshTokenType;
import org.bublapi.dent.auth.repository.RefreshTokenRepository;
import org.bublapi.dent.auth.service.RefreshTokenService;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.InvalidRefreshTokenException;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {
   final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
   final RefreshTokenService service = new RefreshTokenService(repository, 30);
   final Clinic clinic = new Clinic();
   final User user = new User();

   @BeforeEach
   void setUp() {
      clinic.setId(UUID.randomUUID());
      ClinicContext.set(clinic);
      user.setClinic(clinic);
      user.setEnabled(true);
   }

   @AfterEach
   void cleanUp() {
      ClinicContext.clear();
   }

   @Test
   void createsDistinctOpaqueTokensAndStoresOnlyHashes() {
      LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).plusDays(30);
      String first = service.create(user, RefreshTokenType.USER);
      String second = service.create(user, RefreshTokenType.USER);
      var captor = ArgumentCaptor.forClass(RefreshToken.class);
      verify(repository, times(2)).save(captor.capture());
      assertThat(first).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(second);
      assertThat(captor.getAllValues()).extracting(RefreshToken::getTokenHash)
                                       .containsExactly(service.hash(first), service.hash(second));
      assertThat(captor.getValue().getExpiresAt()).isBetween(before, LocalDateTime.now(ZoneOffset.UTC).plusDays(30));
      assertThat(captor.getValue().getUser()).isSameAs(user);
      assertThat(captor.getValue().getTokenType()).isEqualTo(RefreshTokenType.USER);
   }

   @ParameterizedTest
   @ValueSource(strings = {"revoked", "expired", "wrongType", "wrongClinic", "noClinic", "disabled"})
   void rejectsInvalidUserTokenWithoutRevokingOrCreatingReplacement(String reason) {
      RefreshToken token = validToken(RefreshTokenType.USER);
      switch (reason) {
         case "revoked" -> token.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
         case "expired" -> token.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
         case "wrongType" -> token.setTokenType(RefreshTokenType.ADMIN);
         case "wrongClinic" -> ClinicContext.set(new Clinic());
         case "noClinic" -> user.setClinic(null);
         case "disabled" -> user.setEnabled(false);
      }
      LocalDateTime revokedAt = token.getRevokedAt();
      when(repository.findByTokenHashForUpdate(service.hash("raw"))).thenReturn(Optional.of(token));
      assertThatThrownBy(() -> service.rotate("raw", RefreshTokenType.USER)).isInstanceOf(
              InvalidRefreshTokenException.class);
      assertThat(token.getRevokedAt()).isEqualTo(revokedAt);
      verify(repository, never()).save(any());
   }

   @Test
   void unknownTokenCannotRotate() {
      when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.rotate("unknown", RefreshTokenType.USER)).isInstanceOf(
              InvalidRefreshTokenException.class);
      verify(repository, never()).save(any());
   }

   @Test
   void rotationRevokesOldTokenAndRejectsReplay() {
      RefreshToken token = validToken(RefreshTokenType.USER);
      when(repository.findByTokenHashForUpdate(service.hash("raw"))).thenReturn(Optional.of(token));
      var result = service.rotate("raw", RefreshTokenType.USER);
      assertThat(result.user()).isSameAs(user);
      assertThat(result.refreshToken()).isNotBlank().isNotEqualTo("raw");
      assertThat(token.isRevoked()).isTrue();
      assertThatThrownBy(() -> service.rotate("raw", RefreshTokenType.USER)).isInstanceOf(
              InvalidRefreshTokenException.class);
      verify(repository, times(1)).save(any());
   }

   @ParameterizedTest
   @ValueSource(strings = {"valid", "clinicAdmin", "missingRole"})
   void adminRotationRequiresGlobalAdmin(String scenario) {
      Role role = new Role();
      role.setName(scenario.equals("missingRole") ? RoleName.PATIENT : RoleName.ADMIN);
      user.setRoles(Set.of(role));
      if (!scenario.equals("clinicAdmin")) user.setClinic(null);
      RefreshToken token = validToken(RefreshTokenType.ADMIN);
      when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token));
      if (scenario.equals("valid")) {
         assertThat(service.rotate("raw", RefreshTokenType.ADMIN).user()).isSameAs(user);
         assertThat(token.isRevoked()).isTrue();
      } else {
         assertThatThrownBy(() -> service.rotate("raw", RefreshTokenType.ADMIN)).isInstanceOf(
                 InvalidRefreshTokenException.class);
         assertThat(token.isRevoked()).isFalse();
         verify(repository, never()).save(any());
      }
   }

   @Test
   void revokeIsIdempotentAndDoesNotRevokeOtherTokenType() {
      RefreshToken token = validToken(RefreshTokenType.USER);
      when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token));
      service.revoke("raw", RefreshTokenType.ADMIN);
      assertThat(token.isRevoked()).isFalse();
      service.revoke("raw", RefreshTokenType.USER);
      LocalDateTime revokedAt = token.getRevokedAt();
      service.revoke("raw", RefreshTokenType.USER);
      assertThat(token.getRevokedAt()).isEqualTo(revokedAt).isNotNull();
      verify(repository, never()).save(any());
   }

   private RefreshToken validToken(RefreshTokenType type) {
      RefreshToken token = new RefreshToken();
      token.setUser(user);
      token.setTokenType(type);
      token.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
      return token;
   }
}
