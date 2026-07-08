package org.bublapi.dent.integration.appointment;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientAppointmentIntegrationTest extends IntegrationTestSupport {

   @Test
   void patientShouldCreateAppointmentForOwnPatientCard() throws Exception {
      AppointmentContext context = createPatientAppointmentContext();
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createPatientAppointment(context, scheduledAt)
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.patientId").value(context.patient().getId().toString()))
              .andExpect(jsonPath("$.doctorId").value(context.doctor().getId().toString()))
              .andExpect(jsonPath("$.status").value("CREATED"));
   }

   @Test
   void patientWithoutPatientCardShouldNotCreateAppointment() throws Exception {
      TestClinicData clinicData = createClinicData(RoleName.PATIENT);
      AppointmentContext setup = createAppointmentContext(RoleName.OWNER);
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(setup.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      AppointmentContext contextWithoutPatientCard = new AppointmentContext(
              clinicData.clinic(),
              clinicData.user(),
              setup.patient(),
              setup.doctor(),
              setup.clinicService(),
              setup.apiKey()
      );

      createPatientAppointment(contextWithoutPatientCard, scheduledAt)
              .andExpect(status().isForbidden());
   }

   @Test
   void patientShouldCancelOwnAppointment() throws Exception {
      AppointmentContext context = createPatientAppointmentContext();
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      MvcResult createResult = createPatientAppointment(context, scheduledAt).andExpect(status().isOk()).andReturn();
      UUID appointmentId = extractId(createResult);

      mockMvc.perform(patch(PATIENT_APPOINTMENTS_URL + "/{appointmentId}/cancel", appointmentId)
                              .header("Authorization", jwtHelper.token(context.user().getId()))
                              .header("X-API-KEY", context.apiKey().rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.status").value("CANCELLED"));
   }

   @Test
   void patientShouldNotCancelAnotherPatientsAppointment() throws Exception {
      AppointmentContext staffContext = createAppointmentContext(RoleName.OWNER);
      LocalDateTime scheduledAt = futureDateAt(10, 0);
      addRegularWorkingHours(staffContext.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      UUID appointmentId = extractId(createStaffAppointment(staffContext, scheduledAt).andExpect(status().isOk())
                                                                                      .andReturn());

      User otherPatientUser = dataFactory.createUserWithRoles(staffContext.clinic(), "other-patient-" + UUID.randomUUID() + "@test.com", RoleName.PATIENT);
      dataFactory.createPatientForUser(staffContext.clinic(), otherPatientUser);

      mockMvc.perform(patch(PATIENT_APPOINTMENTS_URL + "/{appointmentId}/cancel", appointmentId)
                              .header("Authorization", jwtHelper.token(otherPatientUser.getId()))
                              .header("X-API-KEY", staffContext.apiKey().rawKey()))
             .andExpect(status().isNotFound());
   }
}
