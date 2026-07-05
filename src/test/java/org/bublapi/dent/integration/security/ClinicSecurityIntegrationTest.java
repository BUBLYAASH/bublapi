package org.bublapi.dent.integration.security;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.role.entity.RoleName;
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

      String token = jwtHelper.token(user.getId());

      mockMvc.perform(get("/api/clinics/" + clinicB.getId() + "/patients").header("Authorization", token))
             .andExpect(status().isForbidden());
   }

   @Test
   void shouldAllowAccessToOwnClinic() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User user = dataFactory.createUser(clinic, "user@mail.com");

      String token = jwtHelper.token(user.getId());

      mockMvc.perform(get("/api/public/clinics/" + clinic.getId() + "/doctors").header("Authorization", token))
             .andExpect(status().isOk());
   }

   @Test
   void shouldDenyAccessWithInvalidToken() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User user = dataFactory.createUser(clinic, "user@mail.com");

      String token = jwtHelper.token(user.getId()) + "asd";

      mockMvc.perform(get("/api/patient/clinics/" + clinic.getId() + "/patient-card").header("Authorization", token))
             .andExpect(status().isUnauthorized());
   }

   @Test
   void shouldAllowAdminAcrossClinics() throws Exception {
      Clinic clinicA = dataFactory.createClinic();
      Clinic clinicB = dataFactory.createClinic();

      User admin = dataFactory.createUserWithRoles(clinicA, "admin@mail.com", RoleName.PATIENT, RoleName.ADMIN);

      String token = jwtHelper.token(admin.getId());

      mockMvc.perform(get("/api/clinics/" + clinicB.getId() + "/patients").header("Authorization", token))
             .andExpect(status().isOk());
   }
}