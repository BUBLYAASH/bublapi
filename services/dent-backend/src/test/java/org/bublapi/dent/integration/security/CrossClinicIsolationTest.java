package org.bublapi.dent.integration.security;

import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.appointment.dto.AppointmentServiceRequestDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class CrossClinicIsolationTest extends IntegrationTestBase {

   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;


   @Test
   void shouldNotAllowUserFromClinicAToAccessClinicBPatients() throws Exception {
      Clinic clinicA = dataFactory.createClinic();
      Clinic clinicB = dataFactory.createClinic();

      User receptionistFromClinicA = dataFactory.createUserWithRoles(clinicA, "receptionist-a@test.com",
                                                                     RoleName.RECEPTIONIST);

      dataFactory.createPatient(clinicB);

      CreateApiKeyResponseDto apiKeyClinicB = dataFactory.createApiKey(clinicB);

      mockMvc.perform(get("/api/patients").header("Authorization", jwtHelper.token(receptionistFromClinicA.getId()))
                                          .header("X-API-KEY", apiKeyClinicB.rawKey()))
             .andExpect(status().isForbidden());
   }


   @Test
   void shouldNotAllowUserFromClinicAToModifyClinicBDoctors() throws Exception {
      Clinic clinicA = dataFactory.createClinic();
      Clinic clinicB = dataFactory.createClinic();

      User ownerFromClinicA = dataFactory.createUserWithRoles(clinicA, "owner-a@test.com", RoleName.OWNER);

      Doctor doctorFromClinicB = dataFactory.createDoctor(clinicB);

      String apiKeyClinicA = dataFactory.createApiKey(clinicA).rawKey();

      UpdateDoctorRequestDto request = new UpdateDoctorRequestDto("Hacked", null, null, null, null, null);

      mockMvc.perform(patch("/api/doctors/{doctorId}", doctorFromClinicB.getId()).header("Authorization",
                                                                                         jwtHelper.token(
                                                                                                 ownerFromClinicA.getId()))
                                                                                 .header("X-API-KEY", apiKeyClinicA)
                                                                                 .contentType(
                                                                                         MediaType.APPLICATION_JSON)
                                                                                 .content(
                                                                                         objectMapper.writeValueAsString(
                                                                                                 request)))
             .andExpect(status().isNotFound())
             .andExpect(jsonPath("$.message").value("Doctor in clinic not found"));
   }


   @Test
   void shouldNotAllowUserFromClinicAToCreateAppointmentInClinicB() throws Exception {
      Clinic clinicA = dataFactory.createClinic();
      Clinic clinicB = dataFactory.createClinic();

      User receptionistFromClinicA = dataFactory.createUserWithRoles(clinicA, "receptionist-a@test.com",
                                                                     RoleName.RECEPTIONIST);

      Patient patientFromClinicB = dataFactory.createPatient(clinicB);
      Doctor doctorFromClinicB = dataFactory.createDoctor(clinicB);
      ClinicService clinicServiceFromClinicB = dataFactory.createClinicService(clinicB);

      CreateApiKeyResponseDto apiKeyClinicB = dataFactory.createApiKey(clinicB);

      CreateAppointmentRequestDto request = new CreateAppointmentRequestDto(doctorFromClinicB.getId(),
                                                                            LocalDateTime.now()
                                                                                         .plusDays(1)
                                                                                         .withSecond(0)
                                                                                         .withNano(0),
                                                                            List.of(new AppointmentServiceRequestDto(
                                                                                    clinicServiceFromClinicB.getId(),
                                                                                    1)),
                                                                            "Cross-clinic appointment attempt");

      mockMvc.perform(post("/api/appointments/patients/{patientId}", patientFromClinicB.getId()).header("Authorization",
                                                                                                        jwtHelper.token(
                                                                                                                receptionistFromClinicA.getId()))
                                                                                                .header("X-API-KEY",
                                                                                                        apiKeyClinicB.rawKey())
                                                                                                .contentType(
                                                                                                        MediaType.APPLICATION_JSON)
                                                                                                .content(
                                                                                                        objectMapper.writeValueAsString(
                                                                                                                request)))
             .andExpect(status().isForbidden());
   }


   @Test
   void shouldEnforceClinicFilterInAllQueries() throws Exception {
      Clinic clinicA = dataFactory.createClinic();
      Clinic clinicB = dataFactory.createClinic();

      User ownerFromClinicA = dataFactory.createUserWithRoles(clinicA, "owner-a@test.com", RoleName.OWNER);

      Patient patientFromClinicA = dataFactory.createPatient(clinicA);
      dataFactory.createPatient(clinicB);

      Doctor doctorFromClinicA = dataFactory.createDoctor(clinicA);
      dataFactory.createDoctor(clinicB);

      ClinicService clinicServiceFromClinicA = dataFactory.createClinicService(clinicA);
      dataFactory.createClinicService(clinicB);

      String apiKeyClinicA = dataFactory.createApiKey(clinicA).rawKey();
      String token = jwtHelper.token(ownerFromClinicA.getId());

      mockMvc.perform(get("/api/patients").header("Authorization", token).header("X-API-KEY", apiKeyClinicA))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(patientFromClinicA.getId().toString()))
             .andExpect(jsonPath("$[0].clinicId").value(clinicA.getId().toString()));

      mockMvc.perform(get("/api/doctors").header("Authorization", token).header("X-API-KEY", apiKeyClinicA))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(doctorFromClinicA.getId().toString()))
             .andExpect(jsonPath("$[0].clinicId").value(clinicA.getId().toString()));

      mockMvc.perform(get("/api/services").header("Authorization", token).header("X-API-KEY", apiKeyClinicA))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].id").value(clinicServiceFromClinicA.getId().toString()))
             .andExpect(jsonPath("$[0].clinicId").value(clinicA.getId().toString()));
   }
}