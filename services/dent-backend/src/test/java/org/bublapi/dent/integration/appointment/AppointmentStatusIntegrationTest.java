package org.bublapi.dent.integration.appointment;

import org.bublapi.dent.appointment.dto.ChangeAppointmentStatusRequestDto;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentStatusIntegrationTest extends IntegrationTestSupport {

   @Test
   void shouldConfirmCreatedAppointment() throws Exception {
      AppointmentContext context = createAppointmentWithRegularHours();
      UUID appointmentId = createAppointmentId(context, futureDateAt(10, 0));

      changeStatus(context, appointmentId, AppointmentStatus.CONFIRMED).andExpect(status().isOk())
                                                                       .andExpect(
                                                                               jsonPath("$.status").value("CONFIRMED"));
   }

   @Test
   void shouldCompleteConfirmedAppointment() throws Exception {
      AppointmentContext context = createAppointmentWithRegularHours();
      UUID appointmentId = createAppointmentId(context, futureDateAt(10, 0));

      changeStatus(context, appointmentId, AppointmentStatus.CONFIRMED).andExpect(status().isOk());

      changeStatus(context, appointmentId, AppointmentStatus.COMPLETED).andExpect(status().isOk())
                                                                       .andExpect(
                                                                               jsonPath("$.status").value("COMPLETED"));
   }

   @Test
   void shouldNotCompleteCreatedAppointmentDirectly() throws Exception {
      AppointmentContext context = createAppointmentWithRegularHours();
      UUID appointmentId = createAppointmentId(context, futureDateAt(10, 0));

      changeStatus(context, appointmentId, AppointmentStatus.COMPLETED).andExpect(status().isBadRequest())
                                                                       .andExpect(jsonPath("$.message").value(
                                                                               "Cannot change appointment status from CREATED to COMPLETED"));
   }

   @Test
   void shouldNotChangeCancelledAppointmentStatus() throws Exception {
      AppointmentContext context = createAppointmentWithRegularHours();
      UUID appointmentId = createAppointmentId(context, futureDateAt(10, 0));

      mockMvc.perform(patch(STAFF_APPOINTMENTS_URL + "/{appointmentId}/cancel", appointmentId).header("Authorization",
                                                                                                      jwtHelper.token(
                                                                                                              context.user()
                                                                                                                     .getId()))
                                                                                              .header("X-API-KEY",
                                                                                                      context.apiKey()
                                                                                                             .rawKey()))
             .andExpect(status().isOk());

      changeStatus(context, appointmentId, AppointmentStatus.CONFIRMED).andExpect(status().isBadRequest())
                                                                       .andExpect(jsonPath("$.message").value(
                                                                               "Cannot change appointment status from CANCELLED to CONFIRMED"));
   }

   @Test
   void shouldNotSetSameAppointmentStatus() throws Exception {
      AppointmentContext context = createAppointmentWithRegularHours();
      UUID appointmentId = createAppointmentId(context, futureDateAt(10, 0));

      changeStatus(context, appointmentId, AppointmentStatus.CREATED).andExpect(status().isBadRequest())
                                                                     .andExpect(jsonPath("$.message").value(
                                                                             "Appointment already has this status"));
   }

   @Test
   void patientShouldNotChangeAppointmentStatus() throws Exception {
      AppointmentContext context = createPatientAppointmentContext();
      LocalDateTime scheduledAt = futureDateAt(10, 0);
      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      UUID appointmentId = extractId(
              createPatientAppointment(context, scheduledAt).andExpect(status().isOk()).andReturn());

      ChangeAppointmentStatusRequestDto request = new ChangeAppointmentStatusRequestDto(AppointmentStatus.CONFIRMED);

      mockMvc.perform(patch(STAFF_APPOINTMENTS_URL + "/{appointmentId}/change", appointmentId).header("Authorization",
                                                                                                      jwtHelper.token(
                                                                                                              context.user()
                                                                                                                     .getId()))
                                                                                              .header("X-API-KEY",
                                                                                                      context.apiKey()
                                                                                                             .rawKey())
                                                                                              .contentType(
                                                                                                      MediaType.APPLICATION_JSON)
                                                                                              .content(
                                                                                                      objectMapper.writeValueAsString(
                                                                                                              request)))
             .andExpect(status().isForbidden());
   }

   private AppointmentContext createAppointmentWithRegularHours() {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime scheduledAt = futureDateAt(10, 0);
      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      return context;
   }

   private UUID createAppointmentId(AppointmentContext context, LocalDateTime scheduledAt) throws Exception {
      MvcResult result = createStaffAppointment(context, scheduledAt).andExpect(status().isOk()).andReturn();
      return extractId(result);
   }

   private org.springframework.test.web.servlet.ResultActions changeStatus(AppointmentContext context, UUID appointmentId, AppointmentStatus status) throws
           Exception {
      ChangeAppointmentStatusRequestDto request = new ChangeAppointmentStatusRequestDto(status);

      return mockMvc.perform(
              patch(STAFF_APPOINTMENTS_URL + "/{appointmentId}/change", appointmentId).header("Authorization",
                                                                                              jwtHelper.token(
                                                                                                      context.user()
                                                                                                             .getId()))
                                                                                      .header("X-API-KEY",
                                                                                              context.apiKey().rawKey())
                                                                                      .contentType(
                                                                                              MediaType.APPLICATION_JSON)
                                                                                      .content(
                                                                                              objectMapper.writeValueAsString(
                                                                                                      request)));
   }
}
