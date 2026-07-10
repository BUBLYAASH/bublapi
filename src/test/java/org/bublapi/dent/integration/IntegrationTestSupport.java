package org.bublapi.dent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.bublapi.dent.apikey.dto.CreateApiKeyResponseDto;
import org.bublapi.dent.appointment.dto.AppointmentServiceRequestDto;
import org.bublapi.dent.appointment.dto.CreateAppointmentRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.bublapi.dent.doctor_working_hours.repository.DoctorWorkingHoursRepository;
import org.bublapi.dent.integration.security.TestJwtHelper;
import org.bublapi.dent.integration.testdata.TestDataFactory;
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public abstract class IntegrationTestSupport extends IntegrationTestBase {

   protected static final String STAFF_APPOINTMENTS_URL = "/api/patients/{patientId}/appointments";
   protected static final String PATIENT_APPOINTMENTS_URL = "/api/patient/appointments";
   protected static final DateTimeFormatter RESPONSE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(
           "yyyy-MM-dd'T'HH:mm:ss");

   @Autowired
   protected TestDataFactory dataFactory;

   @Autowired
   protected TestJwtHelper jwtHelper;

   @Autowired
   protected DoctorWorkingHoursRepository doctorWorkingHoursRepository;

   protected TestClinicData createClinicData(RoleName... roleNames) {
      Clinic clinic = dataFactory.createClinic();
      User user = dataFactory.createUserWithRoles(clinic, "test-" + UUID.randomUUID() + "@test.com", roleNames);
      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);
      return new TestClinicData(clinic, user, apiKey);
   }

   protected AppointmentContext createAppointmentContext(RoleName... staffRoles) {
      TestClinicData clinicData = createClinicData(staffRoles);
      Patient patient = dataFactory.createPatient(clinicData.clinic());
      Doctor doctor = dataFactory.createDoctor(clinicData.clinic());
      ClinicService clinicService = dataFactory.createClinicService(clinicData.clinic());
      return new AppointmentContext(clinicData.clinic(), clinicData.user(), patient, doctor, clinicService,
                                    clinicData.apiKey());
   }

   protected AppointmentContext createPatientAppointmentContext() {
      Clinic clinic = dataFactory.createClinic();
      User patientUser = dataFactory.createUserWithRoles(clinic, "patient-" + UUID.randomUUID() + "@test.com",
                                                         RoleName.PATIENT);
      Patient patient = dataFactory.createPatientForUser(clinic, patientUser);
      Doctor doctor = dataFactory.createDoctor(clinic);
      ClinicService clinicService = dataFactory.createClinicService(clinic);
      CreateApiKeyResponseDto apiKey = dataFactory.createApiKey(clinic);
      return new AppointmentContext(clinic, patientUser, patient, doctor, clinicService, apiKey);
   }

   protected void addRegularWorkingHours(Doctor doctor, LocalDate date, LocalTime startTime, LocalTime endTime) {
      DoctorWorkingHours workingHours = new DoctorWorkingHours();
      workingHours.setDoctor(doctor);
      workingHours.setDayOfWeek(DayOfWeek.valueOf(date.getDayOfWeek().name()));
      workingHours.setStartTime(startTime);
      workingHours.setEndTime(endTime);
      doctorWorkingHoursRepository.save(workingHours);
   }

   protected ResultActions createStaffAppointment(AppointmentContext context, LocalDateTime scheduledAt) throws
           Exception {
      return createStaffAppointment(context, scheduledAt,
                                    List.of(new AppointmentServiceRequestDto(context.clinicService()
                                                                                    .getId(), 1)));
   }

   protected ResultActions createStaffAppointment(AppointmentContext context, LocalDateTime scheduledAt, List<AppointmentServiceRequestDto> services) throws
           Exception {
      CreateAppointmentRequestDto request = new CreateAppointmentRequestDto(context.doctor()
                                                                                   .getId(), scheduledAt, services,
                                                                            "Integration test appointment");

      return mockMvc.perform(post(STAFF_APPOINTMENTS_URL, context.patient()
                                                                 .getId()).header("Authorization",
                                                                                  jwtHelper.token(context.user()
                                                                                                         .getId()))
                                                                          .header("X-API-KEY", context.apiKey()
                                                                                                      .rawKey())
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .content(objectMapper.writeValueAsString(
                                                                                  request)));
   }

   protected ResultActions createPatientAppointment(AppointmentContext context, LocalDateTime scheduledAt) throws
           Exception {
      CreateAppointmentRequestDto request = new CreateAppointmentRequestDto(context.doctor()
                                                                                   .getId(), scheduledAt,
                                                                            List.of(new AppointmentServiceRequestDto(
                                                                                    context.clinicService()
                                                                                           .getId(), 1)),
                                                                            "Patient appointment");

      return mockMvc.perform(post(PATIENT_APPOINTMENTS_URL).header("Authorization", jwtHelper.token(context.user()
                                                                                                           .getId()))
                                                           .header("X-API-KEY", context.apiKey().rawKey())
                                                           .contentType(MediaType.APPLICATION_JSON)
                                                           .content(objectMapper.writeValueAsString(request)));
   }

   protected List<AppointmentServiceRequestDto> services(AppointmentServiceRequestDto... services) {
      return Arrays.asList(services);
   }

   protected UUID extractId(MvcResult mvcResult) throws Exception {
      JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
      return UUID.fromString(json.get("id").asText());
   }

   protected String formatResponseDateTime(LocalDateTime value) {
      return value.format(RESPONSE_DATE_TIME_FORMAT);
   }

   protected LocalDateTime futureDateAt(int hour, int minute) {
      return LocalDate.now().plusDays(14).atTime(hour, minute);
   }

   protected record TestClinicData(Clinic clinic, User user, CreateApiKeyResponseDto apiKey) {
   }

   public record AppointmentContext(
           Clinic clinic, User user, Patient patient, Doctor doctor, ClinicService clinicService,
           CreateApiKeyResponseDto apiKey) {
   }
}
