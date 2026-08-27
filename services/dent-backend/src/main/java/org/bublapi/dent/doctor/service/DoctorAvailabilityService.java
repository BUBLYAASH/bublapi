package org.bublapi.dent.doctor.service;

import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.appointment.repository.AppointmentRepository;
import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.doctor.dto.DoctorAvailabilityResponseDto;
import org.bublapi.dent.doctor_schedule_exception.entity.DoctorScheduleException;
import org.bublapi.dent.doctor_schedule_exception.entity.ScheduleExceptionType;
import org.bublapi.dent.doctor_schedule_exception.repository.DoctorScheduleExceptionRepository;
import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.bublapi.dent.doctor_working_hours.repository.DoctorWorkingHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DoctorAvailabilityService {

   private static final int SLOT_STEP_MINUTES = 30;
   private static final int MAX_DAYS = 90;
   private static final int MAX_DURATION_MINUTES = 480;

   private final DoctorService doctorService;
   private final DoctorWorkingHoursRepository workingHoursRepository;
   private final DoctorScheduleExceptionRepository scheduleExceptionRepository;
   private final AppointmentRepository appointmentRepository;

   public DoctorAvailabilityService(DoctorService doctorService, DoctorWorkingHoursRepository workingHoursRepository, DoctorScheduleExceptionRepository scheduleExceptionRepository, AppointmentRepository appointmentRepository) {
      this.doctorService = doctorService;
      this.workingHoursRepository = workingHoursRepository;
      this.scheduleExceptionRepository = scheduleExceptionRepository;
      this.appointmentRepository = appointmentRepository;
   }

   @Transactional(readOnly = true)
   public List<DoctorAvailabilityResponseDto> getAvailability(UUID doctorId, int durationMinutes, int days) {
      validateRequest(durationMinutes, days);

      UUID clinicId = ClinicContext.getClinicId();

      doctorService.findActiveById(doctorId);

      List<DoctorWorkingHours> allWorkingHours = workingHoursRepository.findAllByDoctor_Clinic_IdAndDoctor_Id(clinicId,
                                                                                                              doctorId);

      List<Appointment> doctorAppointments = appointmentRepository.findAllByClinic_IdAndDoctor_IdOrderByScheduledAtAsc(
              clinicId, doctorId);

      LocalDate today = LocalDate.now();
      LocalDateTime now = LocalDateTime.now();

      List<DoctorAvailabilityResponseDto> result = new ArrayList<>();

      for (int dayOffset = 0; dayOffset < days; dayOffset++) {
         LocalDate date = today.plusDays(dayOffset);

         List<TimeInterval> workingIntervals = resolveWorkingIntervals(clinicId, doctorId, date, allWorkingHours);

         if (workingIntervals.isEmpty()) {
            continue;
         }

         List<LocalTime> freeSlots = createFreeSlots(date, workingIntervals, doctorAppointments, durationMinutes, now);

         if (!freeSlots.isEmpty()) {
            result.add(new DoctorAvailabilityResponseDto(date, freeSlots));
         }
      }

      return result;
   }

   private List<TimeInterval> resolveWorkingIntervals(UUID clinicId, UUID doctorId, LocalDate date, List<DoctorWorkingHours> allWorkingHours) {
      List<DoctorScheduleException> exceptions = scheduleExceptionRepository.findAllByDoctor_Clinic_IdAndDoctor_IdAndDate(
              clinicId, doctorId, date);

      boolean hasDayOff = exceptions.stream()
                                    .anyMatch(exception -> exception.getType() == ScheduleExceptionType.DAY_OFF);

      if (hasDayOff) {
         return List.of();
      }

      List<TimeInterval> customWorkingHours = exceptions.stream()
                                                        .filter(exception -> exception.getType() == ScheduleExceptionType.CUSTOM_WORKING_HOURS)
                                                        .filter(exception -> exception.getStartTime() != null && exception.getEndTime() != null)
                                                        .filter(exception -> exception.getStartTime()
                                                                                      .isBefore(exception.getEndTime()))
                                                        .map(exception -> new TimeInterval(exception.getStartTime(),
                                                                                           exception.getEndTime()))
                                                        .sorted(Comparator.comparing(TimeInterval::start))
                                                        .toList();

      if (!customWorkingHours.isEmpty()) {
         return customWorkingHours;
      }

      DayOfWeek requiredDayOfWeek = DayOfWeek.valueOf(date.getDayOfWeek().name());

      return allWorkingHours.stream()
                            .filter(hours -> hours.getDayOfWeek() == requiredDayOfWeek)
                            .filter(hours -> hours.getStartTime() != null && hours.getEndTime() != null)
                            .filter(hours -> hours.getStartTime().isBefore(hours.getEndTime()))
                            .map(hours -> new TimeInterval(hours.getStartTime(), hours.getEndTime()))
                            .sorted(Comparator.comparing(TimeInterval::start))
                            .toList();
   }

   private List<LocalTime> createFreeSlots(LocalDate date, List<TimeInterval> workingIntervals, List<Appointment> appointments, int durationMinutes, LocalDateTime now) {
      List<LocalTime> slots = new ArrayList<>();

      for (TimeInterval workingInterval : workingIntervals) {
         LocalTime slotStart = workingInterval.start();

         while (true) {
            LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

            if (slotEnd.isAfter(workingInterval.end())) {
               break;
            }

            LocalDateTime slotStartDateTime = LocalDateTime.of(date, slotStart);

            LocalDateTime slotEndDateTime = LocalDateTime.of(date, slotEnd);

            boolean isFuture = slotStartDateTime.isAfter(now);

            boolean isFree = !hasOverlappingAppointment(appointments, slotStartDateTime, slotEndDateTime);

            if (isFuture && isFree) {
               slots.add(slotStart);
            }

            slotStart = slotStart.plusMinutes(SLOT_STEP_MINUTES);
         }
      }

      return slots.stream().distinct().sorted().toList();
   }

   private boolean hasOverlappingAppointment(List<Appointment> appointments, LocalDateTime slotStart, LocalDateTime slotEnd) {
      return appointments.stream()
                         .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
                         .filter(appointment -> appointment.getScheduledAt() != null && appointment.getEndAt() != null)
                         .anyMatch(appointment -> intervalsOverlap(slotStart, slotEnd, appointment.getScheduledAt(),
                                                                   appointment.getEndAt()));
   }

   private boolean intervalsOverlap(LocalDateTime firstStart, LocalDateTime firstEnd, LocalDateTime secondStart, LocalDateTime secondEnd) {
      return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
   }

   private void validateRequest(int durationMinutes, int days) {
      if (durationMinutes < 5 || durationMinutes > MAX_DURATION_MINUTES) {
         throw new BadRequestException("Duration must be between 5 and " + MAX_DURATION_MINUTES + " minutes");
      }

      if (days < 1 || days > MAX_DAYS) {
         throw new BadRequestException("Days must be between 1 and " + MAX_DAYS);
      }
   }

   private record TimeInterval(
           LocalTime start, LocalTime end) {
   }
}