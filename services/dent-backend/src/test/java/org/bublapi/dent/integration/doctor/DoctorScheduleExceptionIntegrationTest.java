package org.bublapi.dent.integration.doctor;

import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor_schedule_exception.dto.SetDoctorScheduleExceptionRequestDto;
import org.bublapi.dent.doctor_schedule_exception.entity.ScheduleExceptionType;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorScheduleExceptionIntegrationTest extends IntegrationTestSupport {

   private static final String DOCTOR_URL = "/api/doctors";

   @Test
   void shouldCreateDayOffException() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDate date = LocalDate.now().plusDays(10);

      SetDoctorScheduleExceptionRequestDto request = new SetDoctorScheduleExceptionRequestDto(date,
                                                                                              ScheduleExceptionType.DAY_OFF,
                                                                                              null, null, "Vacation");

      mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/schedule-exceptions", context.doctor().getId())
                              .header("Authorization", jwtHelper.token(context.user().getId()))
                              .header("X-API-KEY", context.apiKey().rawKey())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.doctorId").value(context.doctor().getId().toString()))
             .andExpect(jsonPath("$.date").value(date.toString()))
             .andExpect(jsonPath("$.type").value("DAY_OFF"));
   }

   @Test
   void shouldCreateCustomWorkingHoursException() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDate date = LocalDate.now().plusDays(10);

      SetDoctorScheduleExceptionRequestDto request = new SetDoctorScheduleExceptionRequestDto(date,
                                                                                              ScheduleExceptionType.CUSTOM_WORKING_HOURS,
                                                                                              LocalTime.of(12, 0),
                                                                                              LocalTime.of(16, 0),
                                                                                              "Short day");

      mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/schedule-exceptions", context.doctor().getId())
                              .header("Authorization", jwtHelper.token(context.user().getId()))
                              .header("X-API-KEY", context.apiKey().rawKey())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.type").value("CUSTOM_WORKING_HOURS"))
             .andExpect(jsonPath("$.startTime").value("12:00:00"))
             .andExpect(jsonPath("$.endTime").value("16:00:00"));
   }

   @Test
   void shouldNotCreateCustomWorkingHoursWithoutStartAndEndTime() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDate date = LocalDate.now().plusDays(10);

      SetDoctorScheduleExceptionRequestDto request = new SetDoctorScheduleExceptionRequestDto(date,
                                                                                              ScheduleExceptionType.CUSTOM_WORKING_HOURS,
                                                                                              null, null, "Invalid");

      mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/schedule-exceptions", context.doctor().getId())
                              .header("Authorization", jwtHelper.token(context.user().getId()))
                              .header("X-API-KEY", context.apiKey().rawKey())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.message").value("Schedule exception due to missing start and end time."));
   }

   @Test
   void shouldNotCreateExceptionWhenStartTimeAfterEndTime() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      LocalDate date = LocalDate.now().plusDays(10);

      SetDoctorScheduleExceptionRequestDto request = new SetDoctorScheduleExceptionRequestDto(date,
                                                                                              ScheduleExceptionType.CUSTOM_WORKING_HOURS,
                                                                                              LocalTime.of(18, 0),
                                                                                              LocalTime.of(9, 0),
                                                                                              "Invalid");

      mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/schedule-exceptions", context.doctor().getId())
                              .header("Authorization", jwtHelper.token(context.user().getId()))
                              .header("X-API-KEY", context.apiKey().rawKey())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.message").value("Schedule start time must be before end time."));
   }

   @Test
   void shouldDeleteScheduleException() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      UUID exceptionId = createDayOff(context, context.doctor(), LocalDate.now().plusDays(10));

      mockMvc.perform(delete(DOCTOR_URL + "/{doctorId}/schedule-exceptions/{scheduleExceptionId}", context.doctor()
                                                                                                          .getId(),
                             exceptionId)
                              .header("Authorization", jwtHelper.token(context.user().getId()))
                              .header("X-API-KEY", context.apiKey().rawKey()))
             .andExpect(status().isNoContent());
   }

   @Test
   void shouldNotDeleteScheduleExceptionFromAnotherClinic() throws Exception {
      AppointmentContext clinicA = createAppointmentContext(RoleName.OWNER);
      AppointmentContext clinicB = createAppointmentContext(RoleName.OWNER);
      UUID exceptionId = createDayOff(clinicB, clinicB.doctor(), LocalDate.now().plusDays(10));

      mockMvc.perform(delete(DOCTOR_URL + "/{doctorId}/schedule-exceptions/{scheduleExceptionId}", clinicB.doctor()
                                                                                                          .getId(),
                             exceptionId)
                              .header("Authorization", jwtHelper.token(clinicA.user().getId()))
                              .header("X-API-KEY", clinicA.apiKey().rawKey()))
             .andExpect(status().isNotFound());
   }

   private UUID createDayOff(AppointmentContext context, Doctor doctor, LocalDate date) throws Exception {
      SetDoctorScheduleExceptionRequestDto request = new SetDoctorScheduleExceptionRequestDto(date,
                                                                                              ScheduleExceptionType.DAY_OFF,
                                                                                              null, null, "Day off");
      MvcResult result = mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/schedule-exceptions", doctor.getId())
                                                 .header("Authorization", jwtHelper.token(context.user().getId()))
                                                 .header("X-API-KEY", context.apiKey().rawKey())
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();
      return extractId(result);
   }
}
