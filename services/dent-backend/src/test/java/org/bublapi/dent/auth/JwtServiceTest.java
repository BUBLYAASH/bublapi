package org.bublapi.dent.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.bublapi.dent.auth.service.JwtService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
   static final String SECRET = "test-secret-test-secret-test-secret";
   final JwtService service = new JwtService(SECRET, 900000);

   @Test
   void signedTokenIsBoundToUserAndExpiresAtConfiguredTime() {
      UUID userId = UUID.randomUUID();
      long before = System.currentTimeMillis();
      String token = service.generateToken(userId);
      assertThat(service.extractUserId(token)).isEqualTo(userId.toString());
      assertThat(service.isTokenValid(token, userId)).isTrue();
      assertThat(service.isTokenValid(token, UUID.randomUUID())).isFalse();
      assertThat(service.extractExpiration(token).getTime()).isBetween(before + 899000,
                                                                       System.currentTimeMillis() + 900000);
   }

   @Test
   void rejectsTokenSignedWithAnotherSecret() {
      String token = new JwtService("another-secret-another-secret-123456", 900000).generateToken(UUID.randomUUID());
      assertThatThrownBy(() -> service.extractUserId(token)).isInstanceOf(JwtException.class);
   }

   @Test
   void rejectsExpiredToken() {
      String token = new JwtService(SECRET, -60000).generateToken(UUID.randomUUID());
      assertThatThrownBy(() -> service.extractUserId(token)).isInstanceOf(ExpiredJwtException.class);
   }

   @Test
   void rejectsUnsignedToken() {
      String token = Jwts.builder().subject(UUID.randomUUID().toString()).compact();
      assertThatThrownBy(() -> service.extractUserId(token)).isInstanceOf(JwtException.class);
   }
}
