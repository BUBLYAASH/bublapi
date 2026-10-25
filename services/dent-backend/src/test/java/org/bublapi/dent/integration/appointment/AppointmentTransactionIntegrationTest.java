package org.bublapi.dent.integration.appointment;

import org.bublapi.dent.appointment.dto.AppointmentServiceRequestDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.appointment.repository.AppointmentRepository;
import org.bublapi.dent.appointment.service.AppointmentService;
import org.bublapi.dent.appointment_service.repository.AppointmentServiceRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentTransactionIntegrationTest extends IntegrationTestSupport {
   @Autowired
   AppointmentService service;
   @Autowired
   AppointmentRepository appointments;
   @Autowired
   AppointmentServiceRepository items;

   @Autowired
   org.springframework.jdbc.core.JdbcTemplate jdbc;

   @AfterEach
   void cleanContext() {
      ClinicContext.clear();
   }

   @Test
   void notificationFailureRollsBackAppointmentAndAllServiceItems() {
      var context = createPatientAppointmentContext();
      LocalDateTime at = futureDateAt(10, 0);
      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      var second = dataFactory.createClinicService(context.clinic());
      ClinicContext.set(context.clinic());
      doThrow(new IllegalStateException("notification unavailable")).when(notificationPublisher)
                                                                    .publishAfterCommit(any());
      var request = new CreateAppointmentRequestDto(context.doctor().getId(), at,
                                                    List.of(new AppointmentServiceRequestDto(
                                                                    context.clinicService().getId(), 1),
                                                            new AppointmentServiceRequestDto(second.getId(), 2)),
                                                    "rollback");
      assertThatThrownBy(() -> service.createForPatient(context.user().getId(), request)).isInstanceOf(
              IllegalStateException.class).hasMessage("notification unavailable");
      assertThat(jdbc.queryForObject("select count(*) from appointments", Long.class)).isZero();
      assertThat(jdbc.queryForObject("select count(*) from appointment_services", Long.class)).isZero();
   }

   @Test
   void foreignServiceCannotLeavePartiallyCreatedAppointment() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      var foreign = dataFactory.createClinicService(dataFactory.createClinic());
      createStaffAppointment(context, futureDateAt(10, 0),
                             services(new AppointmentServiceRequestDto(context.clinicService().getId(), 1),
                                      new AppointmentServiceRequestDto(foreign.getId(), 1))).andExpect(
              status().isNotFound());
      assertThat(jdbc.queryForObject("select count(*) from appointments", Long.class)).isZero();
      assertThat(jdbc.queryForObject("select count(*) from appointment_services", Long.class)).isZero();
      verifyNoInteractions(notificationPublisher);
   }

   @Test
   void staffCannotReadCancelOrChangeForeignClinicAppointment() throws Exception {
      var foreign = createAppointmentContext(RoleName.OWNER);
      LocalDateTime at = futureDateAt(10, 0);
      addRegularWorkingHours(foreign.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      UUID id = extractId(createStaffAppointment(foreign, at).andExpect(status().isOk()).andReturn());
      var own = createClinicData(RoleName.OWNER);
      String token = jwtHelper.token(own.user().getId());
      String key = own.apiKey().rawKey();
      mockMvc.perform(get("/api/v1/appointments/" + id).header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNotFound());
      mockMvc.perform(
                     patch("/api/v1/appointments/" + id + "/cancel").header("Authorization", token).header("X-API-KEY", key))
             .andExpect(status().isNotFound());
      mockMvc.perform(patch("/api/v1/appointments/" + id + "/change").header("Authorization", token)
                                                                     .header("X-API-KEY", key)
                                                                     .contentType("application/json")
                                                                     .content("{\"status\":\"CONFIRMED\"}"))
             .andExpect(status().isNotFound());
      ClinicContext.set(foreign.clinic());
      assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo(
              org.bublapi.dent.appointment.entity.AppointmentStatus.CREATED);
   }

   @Test
   void adjacentAppointmentsAreAllowedButOverlapIsRejectedWithoutExtraItems() throws Exception {
      var context = createAppointmentContext(RoleName.RECEPTIONIST);
      LocalDateTime at = futureDateAt(10, 0);
      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      createStaffAppointment(context, at).andExpect(status().isOk());
      createStaffAppointment(context, at.plusMinutes(30)).andExpect(status().isOk());
      createStaffAppointment(context, at.plusMinutes(15)).andExpect(status().isBadRequest());
      assertThat(jdbc.queryForObject("select count(*) from appointments", Long.class)).isEqualTo(2);
      assertThat(jdbc.queryForObject("select count(*) from appointment_services", Long.class)).isEqualTo(2);
   }
}
