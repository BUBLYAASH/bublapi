package org.bublapi.dent.doctor_schedule_exception.service;

import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.doctor_schedule_exception.dto.DoctorScheduleExceptionResponseDto;
import org.bublapi.dent.doctor_schedule_exception.dto.SetDoctorScheduleExceptionRequestDto;
import org.bublapi.dent.doctor_schedule_exception.entity.DoctorScheduleException;
import org.bublapi.dent.doctor_schedule_exception.entity.ScheduleExceptionType;
import org.bublapi.dent.doctor_schedule_exception.mapper.DoctorScheduleExceptionMapper;
import org.bublapi.dent.doctor_schedule_exception.repository.DoctorScheduleExceptionRepository;
import org.bublapi.dent.logging.UserAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class DoctorScheduleExceptionService {
   private final DoctorScheduleExceptionRepository doctorScheduleExceptionRepository;
   private final DoctorRepository doctorRepository;
   private final DoctorScheduleExceptionMapper doctorScheduleExceptionMapper;
   private final UserAuditService userAuditService;

   public DoctorScheduleExceptionService(DoctorScheduleExceptionRepository doctorScheduleExceptionRepository,
                                         DoctorRepository doctorRepository,
                                         DoctorScheduleExceptionMapper doctorScheduleExceptionMapper,
                                         UserAuditService userAuditService) {
      this.doctorScheduleExceptionRepository = doctorScheduleExceptionRepository;
      this.doctorRepository = doctorRepository;
      this.doctorScheduleExceptionMapper = doctorScheduleExceptionMapper;
      this.userAuditService = userAuditService;
   }

   @Transactional
   public DoctorScheduleExceptionResponseDto setException(UUID doctorId, SetDoctorScheduleExceptionRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndIdAndActiveTrue(clinicId, doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found or unavailable"));

      DoctorScheduleException scheduleException = doctorScheduleExceptionMapper.toEntity(request);

      if (request.type() == ScheduleExceptionType.DAY_OFF) {
         scheduleException.setStartTime(LocalTime.MIN);
         scheduleException.setEndTime(LocalTime.MAX);
      } else if (request.type() == ScheduleExceptionType.CUSTOM_WORKING_HOURS) {
         if (scheduleException.getStartTime() == null || scheduleException.getEndTime() == null) {
            throw new BadRequestException("Schedule exception due to missing start and end time.");
         }

         if (!scheduleException.getStartTime().isBefore(scheduleException.getEndTime())) {
            throw new BadRequestException("Schedule start time must be before end time.");
         }
      }

      scheduleException.setDoctor(doctor);

      DoctorScheduleException saved = doctorScheduleExceptionRepository.save(scheduleException);

      userAuditService.doctorScheduleExceptionCreated(saved.getId());

      return doctorScheduleExceptionMapper.toResponse(saved);
   }

   @Transactional
   public void deleteException(UUID doctorId, UUID scheduleExceptionId) {
      UUID clinicId = ClinicContext.getClinicId();

      DoctorScheduleException scheduleException = doctorScheduleExceptionRepository.findByDoctor_Clinic_IdAndIdAndDoctor_Id(
                                                                                           clinicId, scheduleExceptionId, doctorId)
                                                                                   .orElseThrow(
                                                                                           () -> new ResourceNotFoundException(
                                                                                                   "Schedule Exception not found"));

      doctorScheduleExceptionRepository.delete(scheduleException);

      userAuditService.doctorScheduleExceptionDeleted(scheduleException.getId());
   }

   @Transactional
   public List<DoctorScheduleExceptionResponseDto> findAll(UUID doctorId) {
      UUID clinicId = ClinicContext.getClinicId();

      doctorRepository.findByClinic_IdAndId(clinicId, doctorId)
                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

      return doctorScheduleExceptionRepository.findAllByDoctor_Clinic_IdAndDoctor_IdOrderByDateAsc(clinicId, doctorId)
                                              .stream()
                                              .map(doctorScheduleExceptionMapper::toResponse)
                                              .toList();
   }
}
