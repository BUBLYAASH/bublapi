package org.bublapi.dent.integration.security;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleBoundaryIntegrationTest extends IntegrationTestSupport {
   @ParameterizedTest
   @EnumSource(value = RoleName.class, names = {"OWNER", "RECEPTIONIST", "DOCTOR", "PATIENT"})
   void staffListsEnforceRoleMatrix(RoleName role) throws Exception {
      var data = createClinicData(role);
      String token = jwtHelper.token(data.user().getId());
      String key = data.apiKey().rawKey();
      for (String path : new String[]{"/api/v1/patients", "/api/v1/doctors", "/api/v1/users"}) {
         int expected = role == RoleName.OWNER || role == RoleName.RECEPTIONIST ? 200 : 403;
         mockMvc.perform(get(path).header("Authorization", token).header("X-API-KEY", key))
                .andExpect(status().is(expected));
      }
      mockMvc.perform(get("/api/v1/appointments").header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().is(role == RoleName.PATIENT ? 403 : 200));
   }

   @Test
   void removingRoleTakesEffectForAlreadyIssuedJwt() throws Exception {
      var owner = createClinicData(RoleName.OWNER);
      var target = dataFactory.createUserWithRoles(owner.clinic(), "staff@role.test", RoleName.PATIENT,
                                                   RoleName.RECEPTIONIST);
      String token = jwtHelper.token(target.getId());
      mockMvc.perform(
                     get("/api/v1/patients").header("Authorization", token).header("X-API-KEY", owner.apiKey().rawKey()))
             .andExpect(status().isOk());
      mockMvc.perform(delete("/api/v1/users/{userId}/roles/{roleId}", target.getId(),
                             dataFactory.getRole(RoleName.RECEPTIONIST).getId())
                              .header("Authorization", jwtHelper.token(owner.user().getId()))
                              .header("X-API-KEY", owner.apiKey().rawKey()))
             .andExpect(status().isOk());
      mockMvc.perform(
                     get("/api/v1/patients").header("Authorization", token).header("X-API-KEY", owner.apiKey().rawKey()))
             .andExpect(status().isForbidden());
   }

   @Test
   void ownerCannotEscalateUserToAdminOrOwner() throws Exception {
      var owner = createClinicData(RoleName.OWNER);
      var target = dataFactory.createUser(owner.clinic(), "target@role.test");
      for (RoleName forbidden : new RoleName[]{RoleName.ADMIN, RoleName.OWNER}) {
         mockMvc.perform(
                        post("/api/v1/users/{userId}/roles/{roleId}", target.getId(), dataFactory.getRole(forbidden).getId())
                                .header("Authorization", jwtHelper.token(owner.user().getId()))
                                .header("X-API-KEY", owner.apiKey().rawKey()))
                .andExpect(status().isForbidden());
      }
   }
}
