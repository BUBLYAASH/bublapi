package org.bublapi.dent.integration.apikey;

import org.bublapi.dent.apikey.dto.CreateApiKeyRequestDto;
import org.bublapi.dent.apikey.entity.ApiKey;
import org.bublapi.dent.apikey.repository.ApiKeyRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiKeyIntegrationTest extends IntegrationTestSupport {

   @Autowired
   private ApiKeyRepository apiKeyRepository;

   @Test
   void adminShouldCreateApiKeyForClinic() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User admin = dataFactory.createAdmin("admin-" + java.util.UUID.randomUUID() + "@test.com");
      CreateApiKeyRequestDto request = new CreateApiKeyRequestDto("Clinic API key");

      mockMvc.perform(post("/api/admin/clinics/{clinicId}/api-key", clinic.getId()).header("Authorization", jwtHelper.token(admin.getId()))
                                                                                   .contentType(MediaType.APPLICATION_JSON)
                                                                                   .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.rawKey", startsWith("pk_live_")));
   }

   @Test
   void shouldNotCreateSecondActiveApiKeyForSameClinic() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User admin = dataFactory.createAdmin("admin-" + java.util.UUID.randomUUID() + "@test.com");
      CreateApiKeyRequestDto request = new CreateApiKeyRequestDto("Clinic API key");

      mockMvc.perform(post("/api/admin/clinics/{clinicId}/api-key", clinic.getId()).header("Authorization", jwtHelper.token(admin.getId()))
                                                                                   .contentType(MediaType.APPLICATION_JSON)
                                                                                   .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk());

      mockMvc.perform(post("/api/admin/clinics/{clinicId}/api-key", clinic.getId()).header("Authorization", jwtHelper.token(admin.getId()))
                                                                                   .contentType(MediaType.APPLICATION_JSON)
                                                                                   .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().is5xxServerError());
   }

   @Test
   void adminShouldRenewApiKey() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User admin = dataFactory.createAdmin("admin-" + java.util.UUID.randomUUID() + "@test.com");
      dataFactory.createApiKey(clinic);
      ApiKey before = apiKeyRepository.findByClinic_IdAndActiveTrue(clinic.getId()).orElseThrow();
      LocalDateTime oldExpiresAt = before.getExpiresAt();

      mockMvc.perform(patch("/api/admin/clinics/{clinicId}/api-key/renew", clinic.getId()).header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk());

      ApiKey after = apiKeyRepository.findByClinic_IdAndActiveTrue(clinic.getId()).orElseThrow();
      assertThat(after.getExpiresAt()).isAfter(oldExpiresAt);
   }

   @Test
   void requestWithInvalidApiKeyShouldReturnUnauthorized() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);

      mockMvc.perform(get("/api/patients").header("Authorization", jwtHelper.token(data.user().getId()))
                                          .header("X-API-KEY", "bad-key")).andExpect(status().isUnauthorized());
   }

   @Test
   void requestWithoutApiKeyShouldReturnUnauthorized() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);

      mockMvc.perform(get("/api/patients").header("Authorization", jwtHelper.token(data.user().getId())))
             .andExpect(status().isUnauthorized());
   }

   @Test
   void adminEndpointShouldWorkWithoutApiKey() throws Exception {
      User admin = dataFactory.createAdmin("admin-" + java.util.UUID.randomUUID() + "@test.com");

      mockMvc.perform(get("/api/admin/clinics").header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk());
   }
}
