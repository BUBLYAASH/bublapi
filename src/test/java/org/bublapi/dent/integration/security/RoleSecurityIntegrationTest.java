package org.bublapi.dent.integration.security;

import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic_service.dto.AddClinicServiceRequestDto;
import org.bublapi.dent.dental_service.entity.DentalService;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.role.entity.Role;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleSecurityIntegrationTest extends IntegrationTestBase {

   private static final String CLINIC_SERVICES_URL = "/api/services";
   private static final String PATIENTS_URL = "/api/patients";
   private static final String USERS_URL = "/api/users";
   private static final String ADMIN_CLINICS_URL = "/api/admin/clinics";

   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   @Test
   void shouldAllowOwnerToManageClinic() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User owner = dataFactory.createUserWithRoles(clinic, "owner-role-security@test.com", RoleName.OWNER);

      DentalService dentalService = dataFactory.createDentalService();
      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      AddClinicServiceRequestDto request = new AddClinicServiceRequestDto(2_000, 60);

      mockMvc.perform(post(CLINIC_SERVICES_URL + "/{dentalServiceId}", dentalService.getId()).header("Authorization", jwtHelper.token(owner.getId()))
                                                                                             .header("X-API-KEY", apiKey.rawKey())
                                                                                             .contentType(MediaType.APPLICATION_JSON)
                                                                                             .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.clinicId").value(clinic.getId().toString()))
             .andExpect(jsonPath("$.dentalServiceId").value(dentalService.getId().toString()))
             .andExpect(jsonPath("$.price").value(2_000))
             .andExpect(jsonPath("$.durationMinutes").value(60));
   }

   @Test
   void shouldAllowReceptionistToManagePatients() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User receptionist = dataFactory.createUserWithRoles(clinic, "receptionist-role-security@test.com", RoleName.RECEPTIONIST);

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      CreatePatientRequestDto request = new CreatePatientRequestDto("Анна", "Иванова", null, "79991234567", "anna.ivanova@test.com", null, "Первичный приём", null, null);

      mockMvc.perform(post(PATIENTS_URL).header("Authorization", jwtHelper.token(receptionist.getId()))
                                        .header("X-API-KEY", apiKey.rawKey())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.clinicId").value(clinic.getId().toString()))
             .andExpect(jsonPath("$.firstName").value("Анна"))
             .andExpect(jsonPath("$.lastName").value("Иванова"))
             .andExpect(jsonPath("$.phone").value("79991234567"));
   }

   @Test
   void shouldDenyReceptionistRoleAssignment() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User receptionist = dataFactory.createUserWithRoles(clinic, "receptionist-deny-role@test.com", RoleName.RECEPTIONIST);

      User targetUser = dataFactory.createUser(clinic, "target-user-role@test.com");

      Role ownerRole = dataFactory.getRole(RoleName.OWNER);
      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      mockMvc.perform(post(USERS_URL + "/{userId}/roles/{roleId}", targetUser.getId(), ownerRole.getId()).header("Authorization", jwtHelper.token(receptionist.getId()))
                                                                                                         .header("X-API-KEY", apiKey.rawKey()))
             .andExpect(status().isForbidden());
   }

   @Test
   void shouldDenyPatientFromStaffEndpoints() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User patient = dataFactory.createUserWithRoles(clinic, "patient-deny-staff@test.com", RoleName.PATIENT);

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      mockMvc.perform(get(PATIENTS_URL).header("Authorization", jwtHelper.token(patient.getId()))
                                       .header("X-API-KEY", apiKey.rawKey())).andExpect(status().isForbidden());
   }

   @Test
   void shouldAllowAdminWithoutClinic() throws Exception {
      User admin = dataFactory.createAdmin("admin-without-clinic@test.com");

      mockMvc.perform(get(ADMIN_CLINICS_URL).header("Authorization", jwtHelper.token(admin.getId())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$").isArray());
   }
}