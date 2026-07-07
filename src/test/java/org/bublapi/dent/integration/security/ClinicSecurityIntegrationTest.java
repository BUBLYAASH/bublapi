package org.bublapi.dent.integration.security;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicSecurityIntegrationTest extends IntegrationTestBase {

   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   @Test
   void shouldDenyAccessToAnotherClinic() throws Exception {
      Clinic clinicA = dataFactory.createClinic();
      Clinic clinicB = dataFactory.createClinic();

      User user = dataFactory.createUser(clinicA, "user@mail.com");

      String userAToken = jwtHelper.token(user.getId());

      String apiKeyClinicB = dataFactory.createApiKey(clinicB).rawKey();

      mockMvc.perform(get("/api/patients").header("Authorization", userAToken)
                                          .header("X-API-KEY", apiKeyClinicB))
             .andExpect(status().isForbidden());
   }

   @Test
   void shouldDenyAccessWithInvalidToken() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User user = dataFactory.createUser(clinic, "user@mail.com");

      String token = jwtHelper.token(user.getId()) + "asd";

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(get("/api/patient/patient-card").header("Authorization", token).header("X-API-KEY", apiKey))
             .andExpect(status().isUnauthorized());
   }

   @Test
   void adminShouldAccessAnyClinic() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@mail.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(get("/api/patients").header("Authorization", token).header("X-API-KEY", apiKey))
             .andExpect(status().isOk());
   }
}