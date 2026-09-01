package org.bublapi.dent.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class SecurityLogService {

   private static final Logger log = LoggerFactory.getLogger("SECURITY");

   private static String fingerprint(String apiKey) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");

         byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));

         return HexFormat.of().formatHex(hash).substring(0, 16);
      } catch (NoSuchAlgorithmException e) {
         throw new IllegalStateException("SHA-256 is not available", e);
      }
   }

   public void apiKeyAuthenticationFailed(String reason, String apiKey) {
      LoggingEventBuilder event = log.atWarn()
                                     .addKeyValue("eventType", "API_KEY_AUTHENTICATION")
                                     .addKeyValue("result", "FAILURE")
                                     .addKeyValue("reason", reason);

      if (apiKey != null && !apiKey.isBlank()) {
         event.addKeyValue("apiKeyFingerprint", fingerprint(apiKey));
      }

      event.log("API key authentication failed");
   }

   public void apiKeyAuthenticationSuccess(UUID apiKeyId, UUID clinicId) {
      log.atInfo()
         .addKeyValue("eventType", "API_KEY_AUTHENTICATION")
         .addKeyValue("result", "SUCCESS")
         .addKeyValue("apiKeyId", apiKeyId)
         .addKeyValue("clinicId", clinicId)
         .log("API key authentication succeeded");
   }

   public void jwtAuthenticationFailed(String reason) {
      log.atWarn()
         .addKeyValue("eventType", "JWT_AUTHENTICATION")
         .addKeyValue("result", "FAILURE")
         .addKeyValue("reason", reason)
         .log("JWT authentication failed");
   }

   public void jwtAuthenticationSuccess() {
      log.atInfo()
         .addKeyValue("eventType", "JWT_AUTHENTICATION")
         .addKeyValue("result", "SUCCESS")
         .log("JWT authentication succeeded");
   }

   public void loginSuccess(UUID userId, UUID clinicId) {
      log.atInfo()
         .addKeyValue("eventType", "LOGIN")
         .addKeyValue("result", "SUCCESS")
         .addKeyValue("userId", userId)
         .addKeyValue("clinicId", clinicId)
         .log("Login succeeded");
   }

   public void loginFailed(String reason) {
      log.atWarn()
         .addKeyValue("eventType", "LOGIN")
         .addKeyValue("result", "FAILURE")
         .addKeyValue("reason", reason)
         .log("Login failed");
   }

   public void adminLoginSuccess(UUID userId) {
      log.atInfo()
         .addKeyValue("eventType", "ADMIN_LOGIN")
         .addKeyValue("result", "SUCCESS")
         .addKeyValue("userId", userId)
         .log("Admin login succeeded");
   }

   public void adminLoginFailed(String reason) {
      log.atWarn()
         .addKeyValue("eventType", "ADMIN_LOGIN")
         .addKeyValue("result", "FAILURE")
         .addKeyValue("reason", reason)
         .log("Admin login failed");
   }

   public void authenticationRequired() {
      log.atWarn()
         .addKeyValue("eventType", "AUTHENTICATION_REQUIRED")
         .addKeyValue("result", "FAILURE")
         .log("Authentication required");
   }

   public void accessDenied() {
      log.atWarn().addKeyValue("eventType", "ACCESS_DENIED").addKeyValue("result", "FAILURE").log("Access denied");
   }

   public void userDeactivated(UUID targetUserId, UUID clinicId) {
      LoggingEventBuilder event = log.atWarn()
                                     .addKeyValue("eventType", "USER_DEACTIVATED")
                                     .addKeyValue("result", "SUCCESS")
                                     .addKeyValue("targetUserId", targetUserId);

      if (MDC.get("clinicId") == null) {
         event.addKeyValue("clinicId", clinicId);
      }

      event.log("User deactivated");
   }

   public void userActivated(UUID targetUserId, UUID clinicId) {
      LoggingEventBuilder event = log.atInfo()
                                     .addKeyValue("eventType", "USER_ACTIVATED")
                                     .addKeyValue("result", "SUCCESS")
                                     .addKeyValue("targetUserId", targetUserId);

      if (MDC.get("clinicId") == null) {
         event.addKeyValue("clinicId", clinicId);
      }

      event.log("User activated");
   }
}
