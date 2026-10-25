package org.bublapi.dent.integration.appointment;

import org.bublapi.dent.appointment.dto.AppointmentServiceRequestDto;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.assertj.core.api.Assertions.assertThat;

class AppointmentValidationIntegrationTest extends IntegrationTestSupport {
   @Autowired JdbcTemplate jdbc;

   @Test
   void quantityCannotOverflowTotalPriceOrDuration() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      var at = futureDateAt(10, 0);
      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      createStaffAppointment(context, at, List.of(new AppointmentServiceRequestDto(context.clinicService().getId(),
                                                                                   Integer.MAX_VALUE)))
              .andExpect(status().isBadRequest());
   }
   @ParameterizedTest
   @ValueSource(strings = {"priceProduct", "durationProduct", "priceSum", "durationSum"})
   void arithmeticOverflowDoesNotPersistAppointmentOrItems(String scenario) throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      var at = futureDateAt(10, 0);
      var service = context.clinicService();
      boolean priceOverflow = scenario.startsWith("price");
      boolean productOverflow = scenario.endsWith("Product");
      service.setPrice(priceOverflow ? Integer.MAX_VALUE : 1);
      service.setDurationMinutes(priceOverflow ? 30 : Integer.MAX_VALUE);
      assertThat(jdbc.update("update clinic_services set price = ?, duration_minutes = ? where id = ?",
              service.getPrice(), service.getDurationMinutes(), service.getId())).isEqualTo(1);
      var requests = productOverflow
              ? List.of(new AppointmentServiceRequestDto(service.getId(), 2))
              : List.of(new AppointmentServiceRequestDto(service.getId(), 1), new AppointmentServiceRequestDto(service.getId(), 1));
      createStaffAppointment(context, at, requests).andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.message").value("Appointment price or duration exceeds the supported limit"));
      assertThat(jdbc.queryForObject("select count(*) from appointments", Long.class)).isZero();
      assertThat(jdbc.queryForObject("select count(*) from appointment_services", Long.class)).isZero();
   }

   @Test
   void maximumRepresentablePriceRemainsValid() throws Exception {
      var context = createAppointmentContext(RoleName.OWNER);
      var at = futureDateAt(10, 0);
      context.clinicService().setPrice(Integer.MAX_VALUE);
      assertThat(jdbc.update("update clinic_services set price = ? where id = ?",
              Integer.MAX_VALUE, context.clinicService().getId())).isEqualTo(1);
      addRegularWorkingHours(context.doctor(), at.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));
      createStaffAppointment(context, at).andExpect(status().isOk())
              .andExpect(jsonPath("$.totalPrice").value(Integer.MAX_VALUE));
   }

}
