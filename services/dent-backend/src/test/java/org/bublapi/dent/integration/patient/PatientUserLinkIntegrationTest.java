package org.bublapi.dent.integration.patient;

import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.patient.dto.CreatePatientFromProfileRequestDto;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientUserLinkIntegrationTest extends IntegrationTestBase {

   private static final String PATIENT_CARD_URL = "/api/patient/patient-card";

   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   @Autowired
   private PatientRepository patientRepository;

   @Test
   void shouldLinkPatientToUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User user = dataFactory.createUser(clinic, "patient-link@test.com");
      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      CreatePatientFromProfileRequestDto request = new CreatePatientFromProfileRequestDto(LocalDate.of(2004, 5, 12),
                                                                                          "Первичное обращение",
                                                                                          "Аллергия на лидокаин",
                                                                                          "Нет");

      mockMvc.perform(post(PATIENT_CARD_URL).header("Authorization", jwtHelper.token(user.getId()))
                                            .header("X-API-KEY", apiKey.rawKey())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.clinicId").value(clinic.getId().toString()))
             .andExpect(jsonPath("$.userId").value(user.getId().toString()))
             .andExpect(jsonPath("$.birthDate").value("2004-05-12"))
             .andExpect(jsonPath("$.notes").value("Первичное обращение"))
             .andExpect(jsonPath("$.allergies").value("Аллергия на лидокаин"))
             .andExpect(jsonPath("$.chronicDiseases").value("Нет"));
   }

   @Test
   void shouldAutoFillPatientNameFromUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User user = dataFactory.createUser(clinic, "patient-autofill@test.com");

      user.setFirstName("Глеб");
      user.setLastName("Ковалев");
      user.setMiddleName("Максимович");
      user.setPhone("79990001122");
      user.setEmail("gleb.kovalev@test.com");

      user = dataFactory.saveUser(user);

      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      CreatePatientFromProfileRequestDto request = new CreatePatientFromProfileRequestDto(null, null, null, null);

      mockMvc.perform(post(PATIENT_CARD_URL).header("Authorization", jwtHelper.token(user.getId()))
                                            .header("X-API-KEY", apiKey.rawKey())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.userId").value(user.getId().toString()))
             .andExpect(jsonPath("$.firstName").value("Глеб"))
             .andExpect(jsonPath("$.lastName").value("Ковалев"))
             .andExpect(jsonPath("$.middleName").value("Максимович"))
             .andExpect(jsonPath("$.phone").value("79990001122"))
             .andExpect(jsonPath("$.email").value("gleb.kovalev@test.com"));
   }

   @Test
   void shouldAllowUserToHaveOnlyOnePatientProfile() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User user = dataFactory.createUser(clinic, "one-patient-profile@test.com");
      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      CreatePatientFromProfileRequestDto request = new CreatePatientFromProfileRequestDto(LocalDate.of(2000, 1, 1),
                                                                                          "Заметка", null, null);

      mockMvc.perform(post(PATIENT_CARD_URL).header("Authorization", jwtHelper.token(user.getId()))
                                            .header("X-API-KEY", apiKey.rawKey())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk());

      mockMvc.perform(get(PATIENT_CARD_URL).header("Authorization", jwtHelper.token(user.getId()))
                                           .header("X-API-KEY", apiKey.rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.userId").value(user.getId().toString()));
   }

   @Test
   void shouldNotCreateSecondPatientCardForSameUser() throws Exception {
      Clinic clinic = dataFactory.createClinic();
      User user = dataFactory.createUser(clinic, "duplicate-patient-card@test.com");
      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      CreatePatientFromProfileRequestDto firstRequest = new CreatePatientFromProfileRequestDto(
              LocalDate.of(2001, 10, 10), "Первая карточка", null, null);

      CreatePatientFromProfileRequestDto secondRequest = new CreatePatientFromProfileRequestDto(
              LocalDate.of(2002, 11, 11), "Попытка создать вторую карточку", "Нет", "Нет");

      mockMvc.perform(post(PATIENT_CARD_URL).header("Authorization", jwtHelper.token(user.getId()))
                                            .header("X-API-KEY", apiKey.rawKey())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(firstRequest)))
             .andExpect(status().isOk());

      mockMvc.perform(post(PATIENT_CARD_URL).header("Authorization", jwtHelper.token(user.getId()))
                                            .header("X-API-KEY", apiKey.rawKey())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(secondRequest)))
             .andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.message").value("User already has patient card"));
   }
}