package org.bublapi.dent.integration.auth;

import jakarta.servlet.http.Cookie;
import org.bublapi.dent.auth.entity.RefreshToken;
import org.bublapi.dent.auth.entity.RefreshTokenType;
import org.bublapi.dent.auth.repository.RefreshTokenRepository;
import org.bublapi.dent.auth.service.RefreshTokenService;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.service.ClinicOriginService;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.InvalidRefreshTokenException;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshTokenIntegrationTest extends IntegrationTestSupport {
   static final String SITE = "https://refresh.clinic.test";
   static final String ADMIN_SITE = "http://localhost:3001";
   @Autowired
   RefreshTokenRepository tokens;
   @Autowired
   RefreshTokenService refreshTokens;
   @Autowired
   ClinicOriginService origins;

   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void rotationRejectsReplayAndLogoutRevokesReplacement(boolean admin) throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String key = dataFactory.createApiKey(clinic).rawKey();
      origins.add(clinic.getId(), SITE);
      User user = admin ? dataFactory.createAdmin("admin@refresh.test") : dataFactory.createUser(clinic,
                                                                                                 "user@refresh.test");
      String base = admin ? "/api/v1/admin/auth" : "/api/v1/auth";
      String site = admin ? ADMIN_SITE : SITE;
      String name = admin ? "admin_refresh_token" : "refresh_token";
      var login = login(user, key, base, site);
      Cookie oldCookie = login.getCookie(name);
      assertThat(oldCookie).isNotNull();
      assertThat(tokens.findAll()).hasSize(1);
      assertThat(tokens.findAll().getFirst().getTokenHash()).isEqualTo(refreshTokens.hash(oldCookie.getValue()));
      var refreshed = mockMvc.perform(
                                     post(base + "/refresh").header("Origin", site).header("X-API-KEY", key).cookie(oldCookie))
                             .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
                             .andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn().getResponse();
      Cookie replacement = refreshed.getCookie(name);
      assertThat(replacement).isNotNull();
      assertThat(replacement.getValue()).isNotEqualTo(oldCookie.getValue());
      assertThat(tokens.findAll()).hasSize(2).filteredOn(t -> !t.isRevoked()).hasSize(1);
      mockMvc.perform(post(base + "/refresh").header("Origin", site).header("X-API-KEY", key).cookie(oldCookie))
             .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid refresh token"));
      mockMvc.perform(post(base + "/logout").header("Origin", site).header("X-API-KEY", key).cookie(replacement))
             .andExpect(status().isNoContent()).andExpect(cookie().maxAge(name, 0));
      mockMvc.perform(post(base + "/refresh").header("Origin", site).header("X-API-KEY", key).cookie(replacement))
             .andExpect(status().isUnauthorized());
      mockMvc.perform(post(base + "/logout").header("Origin", site).header("X-API-KEY", key).cookie(replacement))
             .andExpect(status().isNoContent());
      assertThat(tokens.findAll()).allMatch(RefreshToken::isRevoked);
   }

   @ParameterizedTest
   @ValueSource(strings = {"expired", "disabled", "wrongClinic", "adminToken", "missing", "blank", "unknown"})
   void failedRefreshCannotConsumeValidTokenOrInsertReplacement(String reason) throws Exception {
      Clinic clinic = dataFactory.createClinic();
      origins.add(clinic.getId(), SITE);
      String key = dataFactory.createApiKey(clinic).rawKey();
      User user = dataFactory.createUser(clinic, "user@refresh.test");
      Cookie cookie = login(user, key, "/api/v1/auth", SITE).getCookie("refresh_token");
      RefreshToken stored = tokens.findAll().getFirst();
      switch (reason) {
         case "expired" -> {
            stored.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            tokens.save(stored);
         }
         case "disabled" -> {
            user.setEnabled(false);
            dataFactory.saveUser(user);
         }
         case "wrongClinic" -> {
            Clinic other = dataFactory.createClinic();
            origins.add(other.getId(), SITE);
            key = dataFactory.createApiKey(other).rawKey();
         }
         case "adminToken" -> {
            stored.setTokenType(RefreshTokenType.ADMIN);
            tokens.save(stored);
         }
         case "blank" -> cookie = new Cookie("refresh_token", " ");
         case "unknown" -> cookie = new Cookie("refresh_token", "unknown");
      }
      var request = post("/api/v1/auth/refresh").header("Origin", SITE).header("X-API-KEY", key);
      if (!reason.equals("missing")) request.cookie(cookie);
      mockMvc.perform(request).andExpect(status().isUnauthorized());
      assertThat(tokens.findAll()).hasSize(1).allMatch(t -> !t.isRevoked());
   }

   @Test
   void userAndAdminCookieNamesCannotBeInterchanged() throws Exception {
      var clinic = dataFactory.createClinic();
      origins.add(clinic.getId(), SITE);
      String key = dataFactory.createApiKey(clinic).rawKey();
      var user = dataFactory.createUser(clinic, "user@refresh.test");
      Cookie cookie = login(user, key, "/api/v1/auth", SITE).getCookie("refresh_token");
      mockMvc.perform(post("/api/v1/admin/auth/refresh").header("Origin", ADMIN_SITE).cookie(cookie))
             .andExpect(status().isUnauthorized());
      assertThat(tokens.findAll()).hasSize(1).allMatch(t -> !t.isRevoked());
   }

   @Test
   void simultaneousRotationHasExactlyOneWinner() throws Exception {
      var clinic = dataFactory.createClinic();
      var user = dataFactory.createUser(clinic, "race@refresh.test");
      String raw = refreshTokens.create(user, RefreshTokenType.USER);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch start = new CountDownLatch(1);
      try {
         Callable<Boolean> rotate = () -> {
            ClinicContext.set(clinic);
            try {
               ready.countDown();
               if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
               refreshTokens.rotate(raw, RefreshTokenType.USER);
               return true;
            } catch (InvalidRefreshTokenException e) {
               return false;
            } finally {
               ClinicContext.clear();
            }
         };
         Future<Boolean> first = executor.submit(rotate);
         Future<Boolean> second = executor.submit(rotate);
         assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
         start.countDown();
         assertThat(
                 List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(
                 true, false);
         assertThat(tokens.findAll()).hasSize(2).filteredOn(t -> !t.isRevoked()).hasSize(1);
      } finally {
         start.countDown();
         executor.shutdownNow();
         assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
      }
   }

   private MockHttpServletResponse login(User user, String key, String base, String site) throws Exception {
      return mockMvc.perform(post(base + "/login").header("Origin", site)
                                                  .header("X-API-KEY", key)
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .content(objectMapper.writeValueAsString(
                                                          Map.of("email", user.getEmail(), "password", "password123"))))
                    .andExpect(status().isOk()).andReturn().getResponse();
   }
}
