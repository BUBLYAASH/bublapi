package org.bublapi.dent.integration.doctor;

import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class DoctorIntegrationTest extends IntegrationTestBase {
   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   private CreateDoctorRequestDto createDoctorRequest() {
      return new CreateDoctorRequestDto("Gregory", "House", "Michael", "Dentist", null, "Experienced dentist");
   }

   private String createDoctor(String token, String apiKey) throws Exception {
      String response = mockMvc.perform(post("/api/doctors").header("Authorization", token)
                                                            .header("X-API-KEY", apiKey)
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .content(objectMapper.writeValueAsString(createDoctorRequest())))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.id", notNullValue()))
                               .andReturn()
                               .getResponse()
                               .getContentAsString();

      return objectMapper.readTree(response).get("id").asText();
   }

   @Test
   void shouldCreateActiveDoctor() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      mockMvc.perform(post("/api/doctors").header("Authorization", token)
                                          .header("X-API-KEY", apiKey)
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(objectMapper.writeValueAsString(createDoctorRequest())))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.active").value(true))
             .andExpect(jsonPath("$.firstName").value("Gregory"));
   }

   @Test
   void shouldCreateInactiveDoctor() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      String doctorId = createDoctor(token, apiKey);

      mockMvc.perform(patch("/api/doctors/" + doctorId + "/deactivation").header("Authorization", token)
                                                                         .header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.active").value(false));
   }


   @Test
   void shouldFindDoctorsByClinic() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();


      createDoctor(token, apiKey);

      mockMvc.perform(get("/api/doctors").header("Authorization", token).header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)));
   }


   @Test
   void shouldFilterInactiveDoctors() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      String doctorId = createDoctor(token, apiKey);

      mockMvc.perform(patch("/api/doctors/" + doctorId + "/deactivation").header("Authorization", token)
                                                                         .header("X-API-KEY", apiKey))
             .andExpect(status().isOk());

      mockMvc.perform(get("/api/public/doctors").header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(0)));
   }

   @Test
   void shouldNotFindInactiveDoctorForAppointment() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      String doctorId = createDoctor(token, apiKey);


      mockMvc.perform(patch("/api/doctors/" + doctorId + "/deactivation").header("Authorization", token)
                                                                         .header("X-API-KEY", apiKey))
             .andExpect(status().isOk());
   }


   @Test
   void shouldUpdateDoctor() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      String doctorId = createDoctor(token, apiKey);

      UpdateDoctorRequestDto request = new UpdateDoctorRequestDto("James", "Wilson", null, "Orthodontist", null, "Updated description");

      mockMvc.perform(patch("/api/doctors/" + doctorId).header("Authorization", token)
                                                       .header("X-API-KEY", apiKey)
                                                       .contentType(MediaType.APPLICATION_JSON)
                                                       .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.firstName").value("James"))
             .andExpect(jsonPath("$.specialty").value("Orthodontist"));
   }


   @Test
   void shouldDeactivateDoctor() throws Exception {
      Clinic clinic = dataFactory.createClinic();

      User admin = dataFactory.createAdmin("admin@test.com");

      String token = jwtHelper.token(admin.getId());

      String apiKey = dataFactory.createApiKey(clinic).rawKey();

      String doctorId = createDoctor(token, apiKey);

      mockMvc.perform(patch("/api/doctors/" + doctorId + "/deactivation").header("Authorization", token)
                                                                         .header("X-API-KEY", apiKey))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.active").value(false));
   }
}