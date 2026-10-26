package org.bublapi.dent.integration.auth;

import org.bublapi.dent.auth.entity.RefreshTokenType;
import org.bublapi.dent.auth.service.AuthService;
import org.bublapi.dent.auth.service.RefreshTokenService;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.repository.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LastLoginIntegrationTest extends IntegrationTestSupport {
   @Autowired
   UserRepository users;
   @Autowired
   AuthService auth;
   @Autowired
   RefreshTokenService tokens;

   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void recordsSuccessfulLoginWithoutChangingProfileUpdateAndRefreshPreservesIt(boolean admin) throws Exception {
      var clinic = dataFactory.createClinic();
      var user = admin ? dataFactory.createAdmin("admin@login.test") : dataFactory.createUserWithRoles(clinic,
                                                                                                       "user@login.test",
                                                                                                       RoleName.OWNER);
      String key = dataFactory.createApiKey(clinic).rawKey();
      var before = users.findById(user.getId()).orElseThrow().getUpdatedAt();
      assertThat(users.findById(user.getId()).orElseThrow().getLastLogin()).isNull();
      var start = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
      mockMvc.perform(post(admin ? "/api/v1/admin/auth/login" : "/api/v1/auth/login")
                              .header("X-API-KEY", key).contentType(MediaType.APPLICATION_JSON)
                              .content(
                                      "{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + TestDataFactory.DEFAULT_PASSWORD + "\"}"))
             .andExpect(status().isOk());
      var saved = users.findById(user.getId()).orElseThrow();
      assertThat(saved.getLastLogin()).isAfter(start).isBefore(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1));
      assertThat(saved.getUpdatedAt()).isEqualTo(before);
      try {
         ClinicContext.set(clinic);
         var type = admin ? RefreshTokenType.ADMIN : RefreshTokenType.USER;
         auth.refresh(tokens.create(saved, type), type);
      } finally {
         ClinicContext.clear();
      }
      assertThat(users.findById(user.getId()).orElseThrow().getLastLogin()).isEqualTo(saved.getLastLogin());
      users.recordLogin(user.getId(), start.minusDays(1));
      assertThat(users.findById(user.getId()).orElseThrow().getLastLogin()).isEqualTo(saved.getLastLogin());
   }

   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void failedAndDisabledLoginsNeverChangeTimestamp(boolean admin) throws Exception {
      var clinic = dataFactory.createClinic();
      var user = admin ? dataFactory.createAdmin("admin@login.test") : dataFactory.createUser(clinic,
                                                                                              "user@login.test");
      String key = dataFactory.createApiKey(clinic).rawKey();
      String base = admin ? "/api/v1/admin/auth/login" : "/api/v1/auth/login";
      var old = LocalDateTime.of(2025, 1, 1, 0, 0);
      users.recordLogin(user.getId(), old);
      mockMvc.perform(post(base).header("X-API-KEY", key).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"wrongpassword\"}"))
             .andExpect(status().isBadRequest());
      mockMvc.perform(post(base).header("X-API-KEY", key).contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"" + UUID.randomUUID() + "@unknown.test\",\"password\":\"wrongpassword\"}"))
             .andExpect(status().isBadRequest());
      user = users.findById(user.getId()).orElseThrow();
      user.setEnabled(false);
      users.saveAndFlush(user);
      mockMvc.perform(post(base).header("X-API-KEY", key).contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + TestDataFactory.DEFAULT_PASSWORD + "\"}"))
             .andExpect(status().isBadRequest());
      assertThat(users.findById(user.getId()).orElseThrow().getLastLogin()).isEqualTo(old);
   }
}
