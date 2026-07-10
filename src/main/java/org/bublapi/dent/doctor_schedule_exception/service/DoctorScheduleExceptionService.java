package org.bublapi.dent.doctor_schedule_exception.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

@Service
public class DoctorScheduleExceptionService {
   private final DoctorScheduleExceptionRepository doctorScheduleExceptionRepository;
   private final DoctorRepository doctorRepository;
   private final DoctorScheduleExceptionMapper doctorScheduleExceptionMapper;

   public DoctorScheduleExceptionService(DoctorScheduleExceptionRepository doctorScheduleExceptionRepository, DoctorRepository doctorRepository, DoctorScheduleExceptionMapper doctorScheduleExceptionMapper) {
      this.doctorScheduleExceptionRepository = doctorScheduleExceptionRepository;
      this.doctorRepository = doctorRepository;
      this.doctorScheduleExceptionMapper = doctorScheduleExceptionMapper;
   }

   @Transactional
   public DoctorScheduleExceptionResponseDto setException(UUID doctorId, SetDoctorScheduleExceptionRequestDto request) {
      Doctor doctor = doctorRepository.findByIdAndActiveTrue(doctorId)
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

      return doctorScheduleExceptionMapper.toResponse(saved);
   }

   public void deleteException(UUID doctorId, UUID scheduleExceptionId) {
      DoctorScheduleException scheduleException = doctorScheduleExceptionRepository.findByIdAndDoctor_Id(
                                                                                           scheduleExceptionId, doctorId)
                                                                                   .orElseThrow(
                                                                                           () -> new ResourceNotFoundException(
                                                                                                   "Schedule Exception not found"));

      doctorScheduleExceptionRepository.delete(scheduleException);
   }
}
