package org.bublapi.dent.integration.security;

import org.bublapi.dent.apikey.repository.ApiKeyRepository;
import org.bublapi.dent.apikey.service.ApiKeyService;
import org.bublapi.dent.clinic.service.ClinicOriginService;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiBoundaryIntegrationTest extends IntegrationTestSupport {
   @Autowired
   ApiKeyRepository keys;
   @Autowired
   ApiKeyService keyService;
   @Autowired
   ClinicOriginService origins;

   @ParameterizedTest
   @ValueSource(strings = {"badSecret", "revoked", "expired", "grace"})
   void publicEndpointsValidateKeySecretStatusAndGracePeriod(String scenario) throws Exception {
      var clinic = dataFactory.createClinic();
      String raw = dataFactory.createApiKey(clinic).rawKey();
      var stored = keys.findByClinic_IdAndActiveTrue(clinic.getId()).orElseThrow();
      switch (scenario) {
         case "badSecret" -> raw = raw.substring(0, raw.length() - 1) + (raw.endsWith("a") ? "b" : "a");
         case "revoked" -> stored.setActive(false);
         case "expired" -> {
            stored.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(15));
            stored.setGraceUntil(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
         }
         case "grace" -> {
            stored.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
            stored.setGraceUntil(LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
         }
      }
      keys.save(stored);
      mockMvc.perform(get("/api/v1/public/doctors").header("X-API-KEY", raw))
             .andExpect(status().is(scenario.equals("grace") ? 200 : 401));
   }

   @Test
   void rotatedKeyStopsWorkingImmediately() throws Exception {
      var clinic = dataFactory.createClinic();
      String old = dataFactory.createApiKey(clinic).rawKey();
      String replacement = keyService.rotate(clinic.getId()).rawKey();
      mockMvc.perform(get("/api/v1/public/doctors").header("X-API-KEY", old)).andExpect(status().isUnauthorized());
      mockMvc.perform(get("/api/v1/public/doctors").header("X-API-KEY", replacement)).andExpect(status().isOk());
   }

   @ParameterizedTest
   @ValueSource(strings = {"missing", "malformed", "disabled", "deleted"})
   void protectedEndpointRejectsInvalidAuthentication(String scenario) throws Exception {
      var data = createClinicData(RoleName.OWNER);
      String token = jwtHelper.token(data.user().getId());
      if (scenario.equals("disabled")) {
         data.user().setEnabled(false);
         dataFactory.saveUser(data.user());
      }
      if (scenario.equals("deleted")) token = jwtHelper.token(UUID.randomUUID());
      if (scenario.equals("malformed")) token = "Bearer invalid.token.signature";
      var request = get("/api/v1/patients").header("X-API-KEY", data.apiKey().rawKey());
      if (!scenario.equals("missing")) request.header("Authorization", token);
      mockMvc.perform(request).andExpect(status().isUnauthorized());
   }

   @ParameterizedTest
   @ValueSource(strings = {"https://unknown.test", "null", "https://allowed.test/path",
                           "https://allowed.test.evil.test"})
   void corsRejectsUnknownOrNonCanonicalOrigins(String origin) throws Exception {
      var clinic = dataFactory.createClinic();
      origins.add(clinic.getId(), "https://allowed.test");
      String key = dataFactory.createApiKey(clinic).rawKey();
      mockMvc.perform(options("/api/v1/public/doctors").header("Origin", origin)
                                                       .header("Access-Control-Request-Method", "GET"))
             .andExpect(status().isForbidden())
             .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
      mockMvc.perform(get("/api/v1/public/doctors").header("Origin", origin).header("X-API-KEY", key))
             .andExpect(status().isForbidden());
   }

   @Test
   void corsAllowsCredentialsOnlyForRegisteredOriginAndPermittedHeaders() throws Exception {
      var clinic = dataFactory.createClinic();
      origins.add(clinic.getId(), "https://allowed.test");
      mockMvc.perform(options("/api/v1/public/doctors").header("Origin", "https://allowed.test")
                                                       .header("Access-Control-Request-Method", "GET")
                                                       .header("Access-Control-Request-Headers",
                                                               "Authorization,X-API-KEY,X-Request-ID"))
             .andExpect(status().isOk())
             .andExpect(header().string("Access-Control-Allow-Origin", "https://allowed.test"))
             .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
      mockMvc.perform(options("/api/v1/public/doctors").header("Origin", "https://allowed.test")
                                                       .header("Access-Control-Request-Method", "TRACE"))
             .andExpect(status().isForbidden());
      mockMvc.perform(options("/api/v1/public/doctors").header("Origin", "https://allowed.test")
                                                       .header("Access-Control-Request-Method", "GET")
                                                       .header("Access-Control-Request-Headers", "X-Evil"))
             .andExpect(status().isForbidden());
   }

   @ParameterizedTest
   @ValueSource(strings = {"/api/v1/admin/api-keys", "/api/v1/admin/users", "/api/v1/admin/notifications",
                           "/api/v1/admin/catalog/dental-services"})
   void clinicOwnerCannotAccessGlobalAdministration(String path) throws Exception {
      var data = createClinicData(RoleName.OWNER);
      mockMvc.perform(get(path).header("Authorization", jwtHelper.token(data.user().getId())))
             .andExpect(status().isForbidden());
   }

   @Test
   void publicQueriesCannotReadForeignClinicResources() throws Exception {
      var a = dataFactory.createClinic();
      var b = dataFactory.createClinic();
      String key = dataFactory.createApiKey(a).rawKey();
      var doctor = dataFactory.createDoctor(b);
      var service = dataFactory.createClinicService(b);
      for (String path : new String[]{"/api/v1/public/doctors/" + doctor.getId(),
                                      "/api/v1/public/doctors/" + doctor.getId() + "/services",
                                      "/api/v1/public/services/" + service.getId(),
                                      "/api/v1/public/services/" + service.getId() + "/doctors"}) {
         mockMvc.perform(get(path).header("X-API-KEY", key)).andExpect(status().isNotFound());
      }
      assertThat(ClinicContext.get()).isNull();
   }
}
