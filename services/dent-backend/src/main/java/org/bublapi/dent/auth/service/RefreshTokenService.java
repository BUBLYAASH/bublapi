package org.bublapi.dent.auth.service;

import org.bublapi.dent.auth.entity.RefreshToken;
import org.bublapi.dent.auth.entity.RefreshTokenType;
import org.bublapi.dent.auth.repository.RefreshTokenRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.InvalidRefreshTokenException;
import org.bublapi.dent.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
   private static final SecureRandom SECURE_RANDOM = new SecureRandom();

   private final RefreshTokenRepository refreshTokenRepository;
   private final long expirationDays;

   public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                              @Value("${jwt.refresh-expiration-days:30}") long expirationTime) {
      this.refreshTokenRepository = refreshTokenRepository;
      this.expirationDays = expirationTime;
   }

   @Transactional
   public String create(User user, RefreshTokenType type) {
      byte[] bytes = new byte[32];
      SECURE_RANDOM.nextBytes(bytes);

      String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

      RefreshToken refreshToken = new RefreshToken();
      refreshToken.setUser(user);
      refreshToken.setTokenType(type);
      refreshToken.setTokenHash(hash(rawToken));
      refreshToken.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));

      refreshTokenRepository.save(refreshToken);

      return rawToken;
   }

   @Transactional
   public RotationResult rotate(String rawToken, RefreshTokenType expectedType) {
      String tokenHash = hash(rawToken);

      RefreshToken oldToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                                                    .orElseThrow(InvalidRefreshTokenException::new);

      if (oldToken.isRevoked() || oldToken.isExpired() || oldToken.getTokenType() != expectedType) {
         throw new InvalidRefreshTokenException();
      }

      User user = oldToken.getUser();

      if (expectedType == RefreshTokenType.USER) {
         UUID clinicId = ClinicContext.getClinicId();

         if (clinicId == null || user.getClinic() == null || !user.getClinic().getId().equals(clinicId)) {
            throw new InvalidRefreshTokenException();
         }
      }

      if (expectedType == RefreshTokenType.ADMIN) {
         boolean isAdmin = user.getClinic() == null && user.getRoles()
                                                           .stream()
                                                           .anyMatch(role -> "ADMIN".equals(role.getName()));

         if (!isAdmin) {
            throw new InvalidRefreshTokenException();
         }
      }

      if (!user.isEnabled()) {
         throw new InvalidRefreshTokenException();
      }

      oldToken.setRevokedAt(LocalDateTime.now());

      String newToken = create(user, expectedType);

      return new RotationResult(user, newToken);
   }

   @Transactional
   public void revoke(String rawToken, RefreshTokenType expectedType) {
      refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken)).ifPresent(token -> {
         if (token.getTokenType() == expectedType && !token.isRevoked()) {
            token.setRevokedAt(LocalDateTime.now());
         }
      });
   }

   public String hash(String token) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");

         byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

         return HexFormat.of().formatHex(hash);
      } catch (NoSuchAlgorithmException e) {
         throw new IllegalStateException("SHA-256 is unavailable", e);
      }
   }

   public record RotationResult(User user, String refreshToken) {
   }
}
