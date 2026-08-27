package org.bublapi.dent.integration.user;

import org.bublapi.dent.auth.dto.LoginRequestDto;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.dto.UpdateUserRequestDto;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileIntegrationTest extends IntegrationTestSupport {

   @Test
   void userShouldUpdateOwnProfile() throws Exception {
      TestClinicData data = createClinicData(RoleName.PATIENT);
      UpdateUserRequestDto request = new UpdateUserRequestDto(
              "updated-" + UUID.randomUUID() + "@test.com",
              "79001234567",
              "Updated",
              "User",
              "Middle",
              null
      );

      mockMvc.perform(patch("/api/profile")
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.firstName").value("Updated"))
             .andExpect(jsonPath("$.lastName").value("User"))
             .andExpect(jsonPath("$.phone").value("79001234567"));
   }

   @Test
   void userShouldDeactivateOwnProfile() throws Exception {
      TestClinicData data = createClinicData(RoleName.PATIENT);

      mockMvc.perform(patch("/api/profile/deactivation")
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.enabled").value(false));
   }

   @Test
   void disabledUserShouldNotLoginAfterProfileDeactivation() throws Exception {
      TestClinicData data = createClinicData(RoleName.PATIENT);

      mockMvc.perform(patch("/api/profile/deactivation")
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk());

      LoginRequestDto request = new LoginRequestDto(data.user().getEmail(), TestDataFactory.DEFAULT_PASSWORD);

      mockMvc.perform(post("/api/auth/login")
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isUnauthorized());
   }

   @Test
   void userShouldNotUpdateProfileWithDuplicateEmailInClinic() throws Exception {
      TestClinicData data = createClinicData(RoleName.PATIENT);
      User other = dataFactory.createUserWithRoles(data.clinic(), "other-" + UUID.randomUUID() + "@test.com",
                                                   RoleName.PATIENT);
      UpdateUserRequestDto request = new UpdateUserRequestDto(other.getEmail(), null, null, null, null, null);

      mockMvc.perform(patch("/api/profile")
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isConflict());
   }

   @Test
   void userShouldNotUpdateProfileWithDuplicatePhoneInClinic() throws Exception {
      TestClinicData data = createClinicData(RoleName.PATIENT);
      User other = dataFactory.createUserWithRoles(data.clinic(), "other-" + UUID.randomUUID() + "@test.com",
                                                   RoleName.PATIENT);
      UpdateUserRequestDto request = new UpdateUserRequestDto(null, other.getPhone(), null, null, null, null);

      mockMvc.perform(patch("/api/profile")
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isConflict());
   }
}
