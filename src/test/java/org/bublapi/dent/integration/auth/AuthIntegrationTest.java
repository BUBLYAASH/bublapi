package org.bublapi.dent.integration.auth;

import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

   @Autowired
   private TestDataFactory dataFactory;

   @Test
   void login_shouldReturnToken() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = UUID.randomUUID() + "@mail.com";

      dataFactory.createUser(clinic, email);

      LoginRequestDto request = new LoginRequestDto(email, TestDataFactory.DEFAULT_PASSWORD);

      mockMvc.perform(post("/api/auth/" + clinic.getId() + "/login").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.token").exists());
   }

   @Test
   void login_shouldReturnBadRequest() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = UUID.randomUUID() + "@mail.com";

      dataFactory.createDisabledUser(clinic, email);

      LoginRequestDto request = new LoginRequestDto(email, TestDataFactory.DEFAULT_PASSWORD);

      mockMvc.perform(post("/api/auth/" + clinic.getId() + "/login").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isBadRequest());
   }

   @Test
   void login_shouldFailWrongPassword() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = UUID.randomUUID() + "@mail.com";

      dataFactory.createUser(clinic, email);

      LoginRequestDto request = new LoginRequestDto(email, "randompassword");

      mockMvc.perform(post("/api/auth/" + clinic.getId() + "/login").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isBadRequest());
   }

   @Test
   void login_shouldFailUnkownEmail() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = UUID.randomUUID() + "@mail.com";

      dataFactory.createUser(clinic, email);

      LoginRequestDto request = new LoginRequestDto("test@mail.com", TestDataFactory.DEFAULT_PASSWORD);

      mockMvc.perform(post("/api/auth/" + clinic.getId() + "/login").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isBadRequest());
   }
}