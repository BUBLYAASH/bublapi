package org.bublapi.dent.integration.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.appointment.dto.AppointmentServiceRequestDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor_schedule_exception.entity.DoctorScheduleException;
import org.bublapi.dent.doctor_schedule_exception.entity.ScheduleExceptionType;
import org.bublapi.dent.doctor_schedule_exception.repository.DoctorScheduleExceptionRepository;
import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.bublapi.dent.doctor_working_hours.repository.DoctorWorkingHoursRepository;
import org.bublapi.dent.integration.IntegrationTestBase;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentAvailabilityIntegrationTest extends IntegrationTestBase {

   private static final String STAFF_APPOINTMENTS_URL = "/api/patients/{patientId}/appointments";
   private final static DateTimeFormatter RESPONSE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

   @Autowired
   private TestDataFactory dataFactory;

   @Autowired
   private TestJwtHelper jwtHelper;

   @Autowired
   private DoctorWorkingHoursRepository doctorWorkingHoursRepository;

   @Autowired
   private DoctorScheduleExceptionRepository doctorScheduleExceptionRepository;

   @Test
   void shouldCreateAppointmentInsideDoctorsWorkingHours() throws Exception {
      TestContext context = createContext();
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createAppointment(context, scheduledAt).andExpect(status().isOk())
                                             .andExpect(jsonPath("$.id", notNullValue()))
                                             .andExpect(jsonPath("$.doctorId").value(context.doctor()
                                                                                            .getId()
                                                                                            .toString()))
                                             .andExpect(jsonPath("$.patientId").value(context.patient()
                                                                                             .getId()
                                                                                             .toString()))
                                             .andExpect(jsonPath("$.scheduledAt").value(formatResponseDateTime(scheduledAt)))
                                             .andExpect(jsonPath("$.endAt").value(formatResponseDateTime(scheduledAt.plusMinutes(30))))
                                             .andExpect(jsonPath("$.totalPrice").value(1_000))
                                             .andExpect(jsonPath("$.status").value("CREATED"));
   }

   @Test
   void shouldNotCreateAppointmentBeforeDoctorsWorkingHours() throws Exception {
      TestContext context = createContext();
      LocalDateTime scheduledAt = futureDateAt(8, 30);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createAppointment(context, scheduledAt).andExpect(status().isBadRequest())
                                             .andExpect(jsonPath("$.message").value("Selected time is outside the doctor's working hours"));
   }

   @Test
   void shouldNotCreateAppointmentAfterDoctorsWorkingHours() throws Exception {
      TestContext context = createContext();
      LocalDateTime scheduledAt = futureDateAt(17, 45);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createAppointment(context, scheduledAt).andExpect(status().isBadRequest())
                                             .andExpect(jsonPath("$.message").value("Selected time is outside the doctor's working hours"));
   }

   @Test
   void shouldNotCreateAppointmentOnDoctorsDayOff() throws Exception {
      TestContext context = createContext();
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      addDayOff(context.doctor(), scheduledAt.toLocalDate());

      createAppointment(context, scheduledAt).andExpect(status().isBadRequest())
                                             .andExpect(jsonPath("$.message").value("Doctor is unavailable on the selected date"));
   }

   @Test
   void shouldCreateAppointmentInsideCustomWorkingHours() throws Exception {
      TestContext context = createContext();
      LocalDateTime scheduledAt = futureDateAt(13, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      addCustomWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(12, 0), LocalTime.of(16, 0));

      createAppointment(context, scheduledAt).andExpect(status().isOk())
                                             .andExpect(jsonPath("$.scheduledAt").value(formatResponseDateTime(scheduledAt)))
                                             .andExpect(jsonPath("$.endAt").value(formatResponseDateTime(scheduledAt.plusMinutes(30))));
   }

   @Test
   void shouldNotCreateAppointmentOutsideCustomWorkingHours() throws Exception {
      TestContext context = createContext();
      LocalDateTime scheduledAt = futureDateAt(11, 30);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      addCustomWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(12, 0), LocalTime.of(16, 0));

      createAppointment(context, scheduledAt).andExpect(status().isBadRequest())
                                             .andExpect(jsonPath("$.message").value("Selected time is outside the doctor's custom working hours"));
   }

   @Test
   void shouldNotCreateOverlappingAppointmentForDoctor() throws Exception {
      TestContext context = createContext();
      LocalDateTime firstAppointmentTime = futureDateAt(10, 0);
      LocalDateTime overlappingAppointmentTime = futureDateAt(10, 15);

      addRegularWorkingHours(context.doctor(), firstAppointmentTime.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createAppointment(context, firstAppointmentTime).andExpect(status().isOk());

      createAppointment(context, overlappingAppointmentTime).andExpect(status().isBadRequest())
                                                            .andExpect(jsonPath("$.message").value("Doctor already has an appointment during the selected time"));
   }

   @Test
   void shouldAllowAppointmentImmediatelyAfterPreviousAppointmentEnds() throws Exception {
      TestContext context = createContext();
      LocalDateTime firstAppointmentTime = futureDateAt(10, 0);
      LocalDateTime secondAppointmentTime = futureDateAt(10, 30);

      addRegularWorkingHours(context.doctor(), firstAppointmentTime.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      createAppointment(context, firstAppointmentTime).andExpect(status().isOk());

      createAppointment(context, secondAppointmentTime).andExpect(status().isOk())
                                                       .andExpect(jsonPath("$.scheduledAt").value(formatResponseDateTime(secondAppointmentTime)))
                                                       .andExpect(jsonPath("$.endAt").value(formatResponseDateTime(secondAppointmentTime.plusMinutes(30))));
   }

   @Test
   void shouldAllowAppointmentAfterCancelledAppointment() throws Exception {
      TestContext context = createContext();
      LocalDateTime scheduledAt = futureDateAt(10, 0);

      addRegularWorkingHours(context.doctor(), scheduledAt.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      MvcResult createResult = createAppointment(context, scheduledAt).andExpect(status().isOk()).andReturn();

      UUID appointmentId = extractId(createResult);

      mockMvc.perform(patch(STAFF_APPOINTMENTS_URL + "/{appointmentId}/cancel", context.patient()
                                                                                       .getId(), appointmentId).header("Authorization", jwtHelper.token(context.staff()
                                                                                                                                                               .getId()))
                                                                                                               .header("X-API-KEY", context.apiKey()
                                                                                                                                           .rawKey()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.status").value("CANCELLED"));

      createAppointment(context, scheduledAt).andExpect(status().isOk())
                                             .andExpect(jsonPath("$.scheduledAt").value(formatResponseDateTime(scheduledAt)));
   }

   @Test
   void shouldNotCreateAppointmentThatEndsOnNextDay() throws Exception {
      TestContext context = createContext();
      LocalDateTime scheduledAt = futureDateAt(23, 45);

      createAppointment(context, scheduledAt).andExpect(status().isBadRequest())
                                             .andExpect(jsonPath("$.message").value("Appointment cannot continue into the next day"));
   }

   private TestContext createContext() {
      Clinic clinic = dataFactory.createClinic();

      User owner = dataFactory.createUserWithRoles(clinic, "appointment-owner-" + UUID.randomUUID() + "@test.com", RoleName.OWNER);

      Patient patient = dataFactory.createPatient(clinic);
      Doctor doctor = dataFactory.createDoctor(clinic);
      ClinicService clinicService = dataFactory.createClinicService(clinic);
      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);

      return new TestContext(clinic, owner, patient, doctor, clinicService, apiKey);
   }

   private void addRegularWorkingHours(Doctor doctor, LocalDate date, LocalTime startTime, LocalTime endTime) {
      DoctorWorkingHours workingHours = new DoctorWorkingHours();

      workingHours.setDoctor(doctor);
      workingHours.setDayOfWeek(DayOfWeek.valueOf(date.getDayOfWeek().name()));
      workingHours.setStartTime(startTime);
      workingHours.setEndTime(endTime);

      doctorWorkingHoursRepository.save(workingHours);
   }

   private void addDayOff(Doctor doctor, LocalDate date) {
      DoctorScheduleException exception = new DoctorScheduleException();

      exception.setDoctor(doctor);
      exception.setDate(date);
      exception.setType(ScheduleExceptionType.DAY_OFF);
      exception.setReason("Doctor day off");

      doctorScheduleExceptionRepository.save(exception);
   }

   private void addCustomWorkingHours(Doctor doctor, LocalDate date, LocalTime startTime, LocalTime endTime) {
      DoctorScheduleException exception = new DoctorScheduleException();

      exception.setDoctor(doctor);
      exception.setDate(date);
      exception.setType(ScheduleExceptionType.CUSTOM_WORKING_HOURS);
      exception.setStartTime(startTime);
      exception.setEndTime(endTime);
      exception.setReason("Custom working hours");

      doctorScheduleExceptionRepository.save(exception);
   }

   private org.springframework.test.web.servlet.ResultActions createAppointment(TestContext context, LocalDateTime scheduledAt) throws
           Exception {
      CreateAppointmentRequestDto request = new CreateAppointmentRequestDto(context.doctor()
                                                                                   .getId(), scheduledAt, List.of(new AppointmentServiceRequestDto(context.clinicService()
                                                                                                                                                          .getId(), 1)), "Integration test appointment");

      return mockMvc.perform(post(STAFF_APPOINTMENTS_URL, context.patient()
                                                                 .getId()).header("Authorization", jwtHelper.token(context.staff()
                                                                                                                          .getId()))
                                                                          .header("X-API-KEY", context.apiKey()
                                                                                                      .rawKey())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content(objectMapper.writeValueAsString(request)));
   }

   private UUID extractId(MvcResult mvcResult) throws Exception {
      JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString());

      return UUID.fromString(json.get("id").asText());
   }

   private LocalDateTime futureDateAt(int hour, int minute) {
      return LocalDate.now().plusDays(14).atTime(hour, minute);
   }

   private record TestContext(
           Clinic clinic, User staff, Patient patient, Doctor doctor, ClinicService clinicService,
           CreateApiKeyResponseDto apiKey) {
   }

   private String formatResponseDateTime(LocalDateTime value) {
      return value.format(RESPONSE_DATE_TIME_FORMAT);
   }
}