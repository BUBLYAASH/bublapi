package org.bublapi.dent.integration.appointment;

import com.jayway.jsonpath.JsonPath;
import org.bublapi.dent.appointment.dto.AppointmentServiceRequestDto;
import org.bublapi.dent.appointment_service.entity.AppointmentServiceItem;
import org.bublapi.dent.appointment_service.repository.AppointmentServiceRepository;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.clinic_service.repository.ClinicServiceRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentLifecycleIntegrationTest extends IntegrationTestSupport {

   @Autowired
   private AppointmentServiceRepository appointmentServiceRepository;

   @Autowired
   private ClinicServiceRepository clinicServiceRepository;

   @Test
   void shouldCreateAppointmentWithSeveralServicesAndQuantities() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      ClinicService secondService = dataFactory.createClinicService(context.clinic());
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createStaffAppointment(context, scheduledAt, services(new AppointmentServiceRequestDto(context.clinicService()
                                                                                                    .getId(), 2),
                                                            new AppointmentServiceRequestDto(secondService.getId(),
                                                                                             1))).andExpect(
                                                                                                         status().isOk())
                                                                                                 .andExpect(jsonPath(
                                                                                                         "$.totalPrice").value(
                                                                                                         3_000))
                                                                                                 .andExpect(jsonPath(
                                                                                                         "$.endAt").value(
                                                                                                         formatResponseDateTime(
                                                                                                                 scheduledAt.plusMinutes(
                                                                                                                         90))))
                                                                                                 .andExpect(jsonPath(
                                                                                                         "$.services",
                                                                                                         hasSize(2)))
                                                                                                 .andExpect(jsonPath(
                                                                                                         "$.services[0].quantity").value(
                                                                                                         2))
                                                                                                 .andExpect(jsonPath(
                                                                                                         "$.services[0].position").value(
                                                                                                         1))
                                                                                                 .andExpect(jsonPath(
                                                                                                         "$.services[1].quantity").value(
                                                                                                         1))
                                                                                                 .andExpect(jsonPath(
                                                                                                         "$.services[1].position").value(
                                                                                                         2));
   }

   @Test
   void shouldSaveServicePriceAndDurationSnapshot() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      MvcResult result = createStaffAppointment(context, scheduledAt)
              .andExpect(status().isOk())
              .andReturn();

      UUID appointmentId = UUID.fromString(
              JsonPath.read(result.getResponse().getContentAsString(), "$.id")
      );

      ClinicContext.set(context.clinic());
      try {
         ClinicService clinicService = clinicServiceRepository.findById(context.clinicService().getId()).orElseThrow();
         clinicService.setPrice(2_000);
         clinicService.setDurationMinutes(60);
         clinicServiceRepository.save(clinicService);
      } finally {
         ClinicContext.clear();
      }

      List<AppointmentServiceItem> items = inClinicContext(context.clinic(),
                                                           () -> appointmentServiceRepository.findAllByAppointment_Id(
                                                                   appointmentId));
      assertThat(items).hasSize(1);
      assertThat(items.getFirst().getPrice()).isEqualTo(1_000);
      assertThat(items.getFirst().getDurationMinutes()).isEqualTo(30);
   }

   private <T> T inClinicContext(Clinic clinic, Supplier<T> action) {
      ClinicContext.set(clinic);
      try {
         return action.get();
      } finally {
         ClinicContext.clear();
      }
   }

   @Test
   void shouldNotCreateAppointmentWithEmptyServices() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      String body = """
              {
                "doctorId": "%s",
                "scheduledAt": "%s",
                "services": [],
                "comment": "empty services"
              }
              """.formatted(context.doctor().getId(), formatResponseDateTime(scheduledAt));

      mockMvc.perform(post(STAFF_PATIENT_APPOINTMENTS_URL, context.patient()
                                                                  .getId()).header("Authorization",
                                                                                   jwtHelper.token(context.user()
                                                                                                          .getId()))
                                                                           .header("X-API-KEY",
                                                                                   context.apiKey().rawKey())
                                                                           .contentType(MediaType.APPLICATION_JSON)
                                                                           .content(body))
             .andExpect(status().isBadRequest());
   }

   @Test
   void shouldNotCreateAppointmentWithZeroQuantity() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createStaffAppointment(context, scheduledAt, services(new AppointmentServiceRequestDto(context.clinicService()
                                                                                                    .getId(),
                                                                                             0))).andExpect(
              status().isBadRequest());
   }

   @Test
   void shouldNotCreateAppointmentWithNegativeQuantity() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createStaffAppointment(context, scheduledAt, services(new AppointmentServiceRequestDto(context.clinicService()
                                                                                                    .getId(),
                                                                                             -1))).andExpect(
              status().isBadRequest());
   }

   @Test
   void shouldNotCreateAppointmentWithMissingQuantity() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      String body = """
              {
                "doctorId": "%s",
                "scheduledAt": "%s",
                "services": [
                  { "clinicServiceId": "%s" }
                ],
                "comment": "missing quantity"
              }
              """.formatted(context.doctor().getId(), formatResponseDateTime(scheduledAt), context.clinicService()
                                                                                                  .getId());

      mockMvc.perform(post(STAFF_PATIENT_APPOINTMENTS_URL, context.patient()
                                                                  .getId()).header("Authorization",
                                                                                   jwtHelper.token(context.user()
                                                                                                          .getId()))
                                                                           .header("X-API-KEY",
                                                                                   context.apiKey().rawKey())
                                                                           .contentType(MediaType.APPLICATION_JSON)
                                                                           .content(body))
             .andExpect(status().isBadRequest());
   }
}
