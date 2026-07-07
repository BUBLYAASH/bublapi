package org.bublapi.dent.integration.patient;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class PatientIntegrationTest extends IntegrationTestBase {
   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   private CreatePatientRequestDto createPatientRequest(String phone) {
      return new CreatePatientRequestDto("John", "Smith", "Middle", phone, "patient@mail.com", null, null, null, null);
   }

   @Test
   void shouldCreatePatient() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(post("/api/patients").header("Authorization", token)
                                           .header("X-API-KEY", apiKey)
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .content(objectMapper.writeValueAsString(createPatientRequest("79991111111"))))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.firstName").value("John"))
             .andExpect(jsonPath("$.phone").value("79991111111"));
   }


   @Test
   void shouldCreatePatientLinkedToUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      User user = dataFactory.createUser(clinic, "patient@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      CreatePatientRequestDto request = new CreatePatientRequestDto(user.getFirstName(), user.getLastName(), user.getMiddleName(), user.getPhone(), user.getEmail(), null, null, null, null);

      mockMvc.perform(post("/api/patients").header("Authorization", token)
                                           .header("X-API-KEY", apiKey)
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.userId").value(user.getId().toString()));
   }


   @Test
   void shouldNotAllowDuplicatePatientPhoneInClinic() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      CreatePatientRequestDto request = createPatientRequest("79992222222");

      mockMvc.perform(post("/api/patients").header("Authorization", token)
                                           .header("X-API-KEY", apiKey)
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk());


      mockMvc.perform(post("/api/patients").header("Authorization", token)
                                           .header("X-API-KEY", apiKey)
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isConflict());
   }


   @Test
   void shouldFindPatientByClinic() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(post("/api/patients").header("Authorization", token)
                                           .header("X-API-KEY", apiKey)
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .content(objectMapper.writeValueAsString(createPatientRequest("79993333333"))))
             .andExpect(status().isOk());

      mockMvc.perform(get("/api/patients").header("Authorization", token).header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)));
   }

   @Test
   void shouldNotAccessPatientFromAnotherClinic() throws Exception {
      Clinic clinicA = dataFactory.createClinic();
      Clinic clinicB = dataFactory.createClinic();

      User receptionist = dataFactory.createUserWithRoles(clinicA, "adminA@test.com", RoleName.RECEPTIONIST);

      String token = jwtHelper.token(receptionist.getId());

      String apiKeyB = dataFactory.createApiKey(clinicB).rawKey();

      mockMvc.perform(get("/api/patients").header("Authorization", token).header("X-API-KEY", apiKeyB))
             .andExpect(status().isForbidden());
   }
}