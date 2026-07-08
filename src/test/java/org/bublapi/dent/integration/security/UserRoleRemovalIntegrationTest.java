package org.bublapi.dent.integration.security;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserRoleRemovalIntegrationTest extends IntegrationTestSupport {

   @Test
   void ownerShouldRemoveReceptionistRole() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      User receptionist = dataFactory.createUserWithRoles(data.clinic(), "receptionist-" + UUID.randomUUID() + "@test.com", RoleName.PATIENT, RoleName.RECEPTIONIST);
      Role receptionistRole = dataFactory.getRole(RoleName.RECEPTIONIST);

      mockMvc.perform(delete("/api/users/{userId}/roles/{roleId}", receptionist.getId(), receptionistRole.getId())
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.userId").value(receptionist.getId().toString()))
             .andExpect(jsonPath("$.roleId").value(receptionistRole.getId().toString()));
   }

   @Test
   void receptionistShouldNotRemoveReceptionistRole() throws Exception {
      TestClinicData data = createClinicData(RoleName.RECEPTIONIST);
      User target = dataFactory.createUserWithRoles(data.clinic(), "target-" + UUID.randomUUID() + "@test.com", RoleName.PATIENT, RoleName.RECEPTIONIST);
      Role receptionistRole = dataFactory.getRole(RoleName.RECEPTIONIST);

      mockMvc.perform(delete("/api/users/{userId}/roles/{roleId}", target.getId(), receptionistRole.getId())
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isForbidden());
   }

   @Test
   void shouldNotRemovePatientRole() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      User target = dataFactory.createUserWithRoles(data.clinic(), "target-" + UUID.randomUUID() + "@test.com", RoleName.PATIENT);
      Role patientRole = dataFactory.getRole(RoleName.PATIENT);

      mockMvc.perform(delete("/api/users/{userId}/roles/{roleId}", target.getId(), patientRole.getId())
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isForbidden());
   }

   @Test
   void shouldNotAssignDuplicateRole() throws Exception {
      TestClinicData data = createClinicData(RoleName.OWNER);
      User target = dataFactory.createUserWithRoles(data.clinic(), "target-" + UUID.randomUUID() + "@test.com", RoleName.PATIENT, RoleName.RECEPTIONIST);
      Role receptionistRole = dataFactory.getRole(RoleName.RECEPTIONIST);

      mockMvc.perform(post("/api/users/{userId}/roles/{roleId}", target.getId(), receptionistRole.getId())
                              .header("Authorization", jwtHelper.token(data.user().getId()))
                              .header("X-API-KEY", data.apiKey().rawKey()))
             .andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.message").value("User already has this role"));
   }
}
