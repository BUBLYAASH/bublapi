package org.bublapi.dent.integration.doctor;

import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor_working_hours.dto.SetDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.dto.UpdateDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;
import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorScheduleIntegrationTest extends IntegrationTestSupport {

   private static final String DOCTOR_URL = "/api/doctors";

   @Test
   void shouldCreateDoctorWorkingHours() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);

      SetDoctorWorkingHoursRequestDto request = new SetDoctorWorkingHoursRequestDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

      mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/working-hours", context.doctor()
                                                                            .getId()).header("Authorization", jwtHelper.token(context.user()
                                                                                                                                     .getId()))
                                                                                     .header("X-API-KEY", context.apiKey()
                                                                                                                 .rawKey())
                                                                                     .contentType(MediaType.APPLICATION_JSON)
                                                                                     .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id", notNullValue()))
             .andExpect(jsonPath("$.doctorId").value(context.doctor().getId().toString()))
             .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"));
   }

   @Test
   void shouldUpdateDoctorWorkingHours() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      UUID scheduleId = createSchedule(context.doctor(), context, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

      UpdateDoctorWorkingHoursRequestDto request = new UpdateDoctorWorkingHoursRequestDto(LocalTime.of(10, 0), LocalTime.of(16, 0));

      mockMvc.perform(patch(DOCTOR_URL + "/{doctorId}/working-hours/{scheduleId}", context.doctor()
                                                                                          .getId(), scheduleId).header("Authorization", jwtHelper.token(context.user()
                                                                                                                                                               .getId()))
                                                                                                               .header("X-API-KEY", context.apiKey()
                                                                                                                                           .rawKey())
                                                                                                               .contentType(MediaType.APPLICATION_JSON)
                                                                                                               .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.startTime").value("10:00:00"))
             .andExpect(jsonPath("$.endTime").value("16:00:00"));
   }

   @Test
   void shouldDeleteDoctorWorkingHours() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      UUID scheduleId = createSchedule(context.doctor(), context, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

      mockMvc.perform(delete(DOCTOR_URL + "/{doctorId}/working-hours/{scheduleId}", context.doctor()
                                                                                           .getId(), scheduleId).header("Authorization", jwtHelper.token(context.user()
                                                                                                                                                                .getId()))
                                                                                                                .header("X-API-KEY", context.apiKey()
                                                                                                                                            .rawKey()))
             .andExpect(status().isNoContent());

      mockMvc.perform(get("/api/public/doctors/{doctorId}/working-hours", context.doctor()
                                                                                 .getId()).header("X-API-KEY", context.apiKey()
                                                                                                                      .rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(0)));
   }

   @Test
   void shouldNotCreateWorkingHoursForInactiveDoctor() throws Exception {
      TestClinicData clinicData = createClinicData(RoleName.OWNER);
      Doctor inactiveDoctor = dataFactory.createInactiveDoctor(clinicData.clinic());
      SetDoctorWorkingHoursRequestDto request = new SetDoctorWorkingHoursRequestDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

      mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/working-hours", inactiveDoctor.getId()).header("Authorization", jwtHelper.token(clinicData.user()
                                                                                                                                               .getId()))
                                                                                            .header("X-API-KEY", clinicData.apiKey()
                                                                                                                           .rawKey())
                                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                                            .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isNotFound());
   }

   @Test
   void shouldNotCreateWorkingHoursWhenStartTimeAfterEndTime() throws Exception {
      AppointmentContext context = createAppointmentContext(RoleName.OWNER);
      SetDoctorWorkingHoursRequestDto request = new SetDoctorWorkingHoursRequestDto(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(9, 0));

      mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/working-hours", context.doctor()
                                                                            .getId()).header("Authorization", jwtHelper.token(context.user()
                                                                                                                                     .getId()))
                                                                                     .header("X-API-KEY", context.apiKey()
                                                                                                                 .rawKey())
                                                                                     .contentType(MediaType.APPLICATION_JSON)
                                                                                     .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isBadRequest());
   }

   @Test
   void shouldNotUpdateWorkingHoursFromAnotherClinic() throws Exception {
      AppointmentContext clinicA = createAppointmentContext(RoleName.OWNER);
      AppointmentContext clinicB = createAppointmentContext(RoleName.OWNER);
      UUID scheduleId = createSchedule(clinicB.doctor(), clinicB, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

      UpdateDoctorWorkingHoursRequestDto request = new UpdateDoctorWorkingHoursRequestDto(LocalTime.of(10, 0), LocalTime.of(16, 0));

      mockMvc.perform(patch(DOCTOR_URL + "/{doctorId}/working-hours/{scheduleId}", clinicB.doctor()
                                                                                          .getId(), scheduleId).header("Authorization", jwtHelper.token(clinicA.user()
                                                                                                                                                               .getId()))
                                                                                                               .header("X-API-KEY", clinicA.apiKey()
                                                                                                                                           .rawKey())
                                                                                                               .contentType(MediaType.APPLICATION_JSON)
                                                                                                               .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isNotFound());
   }

   private UUID createSchedule(Doctor doctor, AppointmentContext context, DayOfWeek dayOfWeek, LocalTime start, LocalTime end) throws
           Exception {
      SetDoctorWorkingHoursRequestDto request = new SetDoctorWorkingHoursRequestDto(dayOfWeek, start, end);
      MvcResult result = mockMvc.perform(post(DOCTOR_URL + "/{doctorId}/working-hours", doctor.getId()).header("Authorization", jwtHelper.token(context.user()
                                                                                                                                                       .getId()))
                                                                                                       .header("X-API-KEY", context.apiKey()
                                                                                                                                   .rawKey())
                                                                                                       .contentType(MediaType.APPLICATION_JSON)
                                                                                                       .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();
      return extractId(result);
   }
}
