package org.bublapi.dent.integration.user;

import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.auth.dto.RegisterRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserIntegrationTest extends IntegrationTestBase {
   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   @Test
   void login_shouldReturnToken() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = UUID.randomUUID() + "@mail.com";

      dataFactory.createUser(clinic, email);

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      LoginRequestDto request = new LoginRequestDto(email, TestDataFactory.DEFAULT_PASSWORD);

      mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                             .content(objectMapper.writeValueAsString(request))
                                             .header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.token").exists());
   }

   @Test
   void shouldReturnUserWithSingleRole() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = "user_with_single_role@mail.com";

      User user = dataFactory.createUserWithRoles(clinic, email, RoleName.RECEPTIONIST);

      User adminUser = dataFactory.createAdmin("admin@mail.com");

      String adminToken = jwtHelper.token(adminUser.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(get("/api/users/" + user.getId()).header("Authorization", adminToken).header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.roles", hasItem("RECEPTIONIST")))
             .andExpect(jsonPath("$.roles.length()").value(1));
   }

   @Test
   void shouldReturnUserWithMultipleRoles() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = "user_with_multiple_roles@mail.com";

      User user = dataFactory.createUserWithRoles(clinic, email, RoleName.PATIENT, RoleName.RECEPTIONIST);

      User adminUser = dataFactory.createAdmin("admin@mail.com");

      String adminToken = jwtHelper.token(adminUser.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(get("/api/users/" + user.getId()).header("Authorization", adminToken).header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.roles", hasItems("PATIENT", "RECEPTIONIST")))
             .andExpect(jsonPath("$.roles.length()").value(2));
   }

   @Test
   void shouldReturnDisabledUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = "disabled_user@mail.com";

      User user = dataFactory.createDisabledUser(clinic, email);

      User adminUser = dataFactory.createAdmin("admin@mail.com");

      String adminToken = jwtHelper.token(adminUser.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(get("/api/users/" + user.getId()).header("Authorization", adminToken).header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.enabled").value(false));
   }

   @Test
   void shouldReturnDeactivatedUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = "user@mail.com";

      User user = dataFactory.createUser(clinic, email);

      User adminUser = dataFactory.createAdmin("admin@mail.com");

      String adminToken = jwtHelper.token(adminUser.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(patch("/api/users/" + user.getId() + "/deactivation").header("Authorization", adminToken)
                                                                           .header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.enabled").value(false));
   }

   @Test
   void shouldReturnActivatedUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = "disabled_user@mail.com";

      User user = dataFactory.createDisabledUser(clinic, email);

      User adminUser = dataFactory.createAdmin("admin@mail.com");

      String adminToken = jwtHelper.token(adminUser.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(patch("/api/users/" + user.getId() + "/activation").header("Authorization", adminToken)
                                                                         .header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.enabled").value(true));
   }

   @Test
   void ShouldNotAutenticateDisabledUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      String email = "disabled_user@mail.com";

      dataFactory.createDisabledUser(clinic, email);

      LoginRequestDto request = new LoginRequestDto(email, TestDataFactory.DEFAULT_PASSWORD);

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(post("/api/auth/login").content(objectMapper.writeValueAsString(request))
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .header("X-API-KEY", apiKey)).andExpect(status().isBadRequest());
   }

   @Test
   void shouldSaveUniquePhonePerUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      RegisterRequestDto firstRequest = new RegisterRequestDto("User1FirstName", "User1LastName", "User1MiddleName", "user1@mail.ru", "79314056195", TestDataFactory.DEFAULT_PASSWORD);
      RegisterRequestDto duplicatePhoneRequest = new RegisterRequestDto("User2FirstName", "User2LastName", "User2MiddleName", "user2@mail.ru", "79314056195", TestDataFactory.DEFAULT_PASSWORD);
      RegisterRequestDto uniquePhoneRequest = new RegisterRequestDto("User2FirstName", "User2LastName", "User2MiddleName", "user2@mail.ru", "79314056196", TestDataFactory.DEFAULT_PASSWORD);

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(post("/api/auth/register").content(objectMapper.writeValueAsString(firstRequest))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("X-API-KEY", apiKey)).andExpect(status().isOk());

      mockMvc.perform(post("/api/auth/register").content(objectMapper.writeValueAsString(duplicatePhoneRequest))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("X-API-KEY", apiKey)).andExpect(status().isConflict());

      mockMvc.perform(post("/api/auth/register").content(objectMapper.writeValueAsString(uniquePhoneRequest))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("X-API-KEY", apiKey)).andExpect(status().isOk());
   }

   @Test
   void adminShouldAccessWithoutApiKey() throws Exception {
      User admin = dataFactory.createAdmin("admin@mail.com");
      mockMvc.perform(get("/api/admin/clinics").header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk());
   }
}
