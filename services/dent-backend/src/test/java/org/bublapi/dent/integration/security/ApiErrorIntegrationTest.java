package org.bublapi.dent.integration.security;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiErrorIntegrationTest extends IntegrationTestSupport {
   @ParameterizedTest
   @ValueSource(strings = {"{", "{\"email\":", "[]"})
   void malformedLoginBodyReturnsClientError(String body) throws Exception {
      var data = createClinicData(RoleName.PATIENT);
      mockMvc.perform(post("/api/v1/auth/login").header("X-API-KEY", data.apiKey().rawKey())
                                                .contentType("application/json")
                                                .content(body)).andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.status").value(400))
             .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
             .andExpect(jsonPath("$.stackTrace").doesNotExist());
   }

   @Test
   void malformedResourceIdReturnsClientError() throws Exception {
      var data = createClinicData(RoleName.OWNER);
      mockMvc.perform(get("/api/v1/doctors/not-a-uuid").header("Authorization", jwtHelper.token(data.user().getId()))
                                                       .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isBadRequest());
   }

   @Test
   void validationErrorHasStructuredFieldMessagesAndNoInternalDetails() throws Exception {
      var data = createClinicData(RoleName.PATIENT);
      mockMvc.perform(post("/api/v1/auth/login").header("X-API-KEY", data.apiKey().rawKey())
                                                .contentType("application/json")
                                                .content("{\"email\":\"bad-email\",\"password\":\"\"}"))
             .andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.status").value(400))
             .andExpect(jsonPath("$.validationErrors.email").isNotEmpty())
             .andExpect(jsonPath("$.validationErrors.password").isNotEmpty())
             .andExpect(jsonPath("$.timestamp").exists())
             .andExpect(jsonPath("$.stackTrace").doesNotExist());
   }
}
