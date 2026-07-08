package org.bublapi.dent.integration.clinic;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.bublapi.dent.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicCascadeIntegrationTest extends IntegrationTestSupport {

   @Autowired
   private UserRepository userRepository;

   @Test
   void shouldDisableClinicUsersWhenClinicDeactivated() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User admin = dataFactory.createAdmin("admin-" + java.util.UUID.randomUUID() + "@test.com");
      User owner = dataFactory.createUserWithRoles(clinic, "owner-" + java.util.UUID.randomUUID() + "@test.com", RoleName.OWNER);

      mockMvc.perform(patch("/api/admin/clinics/{clinicId}/deactivation", clinic.getId()).header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk());

      User updated = userRepository.findById(owner.getId()).orElseThrow();
      assertThat(updated.getEnabled()).isFalse();
      assertThat(updated.getDisabledByClinic()).isTrue();
   }

   @Test
   void shouldEnableClinicUsersWhenClinicActivated() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User admin = dataFactory.createAdmin("admin-" + java.util.UUID.randomUUID() + "@test.com");
      User owner = dataFactory.createUserWithRoles(clinic, "owner-" + java.util.UUID.randomUUID() + "@test.com", RoleName.OWNER);

      assertThat(admin.getRoles())
              .extracting(role -> role.getName())
              .contains(RoleName.ADMIN);

      User savedAdmin = userRepository.findById(admin.getId()).orElseThrow();

      mockMvc.perform(patch("/api/admin/clinics/{clinicId}/deactivation", clinic.getId()).header("Authorization", jwtHelper.token(savedAdmin.getId())))
             .andExpect(status().isOk());

      User adminAfterDeactivation = userRepository.findById(savedAdmin.getId()).orElseThrow();

      User ownerAfterDeactivation = userRepository.findById(owner.getId()).orElseThrow();

      System.out.println("admin enabled = " + adminAfterDeactivation.getEnabled());

      System.out.println("admin clinic = " + (adminAfterDeactivation.getClinic() == null ? null : adminAfterDeactivation.getClinic()
                                                                                                                        .getId()));

      System.out.println("owner enabled = " + ownerAfterDeactivation.getEnabled());

      System.out.println("owner clinic = " + ownerAfterDeactivation.getClinic().getId());

      mockMvc.perform(patch("/api/admin/clinics/{clinicId}/activation", clinic.getId()).header("Authorization", jwtHelper.token(savedAdmin.getId())))
             .andExpect(status().isOk());

      User updated = userRepository.findById(owner.getId()).orElseThrow();
      assertThat(updated.getEnabled()).isTrue();
      assertThat(updated.getDisabledByClinic()).isFalse();
   }

   @Test
   void deactivatedClinicApiKeyShouldNotAllowAccess() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User admin = dataFactory.createAdmin("admin-" + java.util.UUID.randomUUID() + "@test.com");
      User owner = dataFactory.createUserWithRoles(clinic, "owner-" + java.util.UUID.randomUUID() + "@test.com", RoleName.OWNER);
      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(patch("/api/admin/clinics/{clinicId}/deactivation", clinic.getId()).header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk());

      mockMvc.perform(get("/api/patients").header("Authorization", jwtHelper.token(owner.getId()))
                                          .header("X-API-KEY", apiKey)).andExpect(status().isForbidden());
   }
}
