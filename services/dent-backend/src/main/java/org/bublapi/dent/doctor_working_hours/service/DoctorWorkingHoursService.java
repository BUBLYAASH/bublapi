package org.bublapi.dent.doctor_working_hours.service;

import org.bublapi.dent.common.context.ClinicContext;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.doctor_working_hours.dto.DoctorWorkingHoursResponseDto;
import org.bublapi.dent.doctor_working_hours.dto.SetDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.dto.UpdateDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.bublapi.dent.doctor_working_hours.mapper.DoctorWorkingHoursMapper;
import org.bublapi.dent.doctor_working_hours.repository.DoctorWorkingHoursRepository;
import org.bublapi.dent.logging.UserAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DoctorWorkingHoursService {
   private final DoctorWorkingHoursRepository doctorWorkingHoursRepository;
   private final DoctorRepository doctorRepository;
   private final DoctorWorkingHoursMapper doctorWorkingHoursMapper;
   private final UserAuditService userAuditService;

   public DoctorWorkingHoursService(DoctorWorkingHoursRepository doctorWorkingHoursRepository,
                                    DoctorRepository doctorRepository,
                                    DoctorWorkingHoursMapper doctorWorkingHoursMapper,
                                    UserAuditService userAuditService) {
      this.doctorWorkingHoursRepository = doctorWorkingHoursRepository;
      this.doctorRepository = doctorRepository;
      this.doctorWorkingHoursMapper = doctorWorkingHoursMapper;
      this.userAuditService = userAuditService;
   }

   private void validateTimeRange(LocalTime start, LocalTime end) {
      if (start == null || end == null) {
         throw new BadRequestException("Start time and end time are required");
      }

      if (!start.isBefore(end)) {
         throw new BadRequestException("Start time must be before end time");
      }
   }

   @Transactional
   public DoctorWorkingHoursResponseDto setSchedule(UUID doctorId, SetDoctorWorkingHoursRequestDto request) {
      validateTimeRange(request.startTime(), request.endTime());

      UUID clinicId = ClinicContext.getClinicId();

      Doctor doctor = doctorRepository.findByClinic_IdAndIdAndActiveTrue(clinicId, doctorId)
                                      .orElseThrow(
                                              () -> new ResourceNotFoundException("Doctor not found or unavailable"));

      boolean overlaps = doctorWorkingHoursRepository.existsOverlappingInterval(clinicId, doctorId, request.dayOfWeek(),
                                                                                request.startTime(), request.endTime());

      if (overlaps) {
         throw new BadRequestException("Overlaps an existing working interval");
      }

      DoctorWorkingHours workingHours = doctorWorkingHoursMapper.toEntity(request);

      workingHours.setDoctor(doctor);

      DoctorWorkingHours saved = doctorWorkingHoursRepository.save(workingHours);

      userAuditService.doctorWorkingHoursCreated(saved.getId());

      return doctorWorkingHoursMapper.toResponse(saved);
   }

   @Transactional
   public DoctorWorkingHoursResponseDto updateSchedule(UUID doctorId, UUID scheduleId,
                                                       UpdateDoctorWorkingHoursRequestDto request) {
      UUID clinicId = ClinicContext.getClinicId();

      doctorRepository.findByClinic_IdAndIdAndActiveTrue(clinicId, doctorId)
                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      DoctorWorkingHours workingHours = doctorWorkingHoursRepository.findByDoctor_Clinic_IdAndIdAndDoctor_Id(clinicId,
                                                                                                             scheduleId,
                                                                                                             doctorId)
                                                                    .orElseThrow(() -> new ResourceNotFoundException(
                                                                            "This schedule for doctor not found or unavailable"));

      validateTimeRange(request.startTime(), request.endTime());

      boolean overlaps = doctorWorkingHoursRepository.existsOverlappingIntervalExcept(clinicId, doctorId, scheduleId,
                                                                                      workingHours.getDayOfWeek(),
                                                                                      request.startTime(),
                                                                                      request.endTime());

      if (overlaps) {
         throw new BadRequestException("Overlaps an existing working interval");
      }

      List<String> changedFields = new ArrayList<>();

      if (!Objects.equals(workingHours.getStartTime(), request.startTime())) {
         changedFields.add("startTime");
      }

      if (!Objects.equals(workingHours.getEndTime(), request.endTime())) {
         changedFields.add("endTime");
      }

      doctorWorkingHoursMapper.updateEntity(request, workingHours);

      userAuditService.doctorWorkingHoursUpdated(workingHours.getId(), changedFields);

      return doctorWorkingHoursMapper.toResponse(workingHours);
   }

   public List<DoctorWorkingHoursResponseDto> getSchedule(UUID doctorId) {
      UUID clinicId = ClinicContext.getClinicId();

      doctorRepository.findByClinic_IdAndIdAndActiveTrue(clinicId, doctorId)
                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      return doctorWorkingHoursRepository.findAllByDoctor_Clinic_IdAndDoctor_Id(clinicId, doctorId)
                                         .stream()
                                         .map(doctorWorkingHoursMapper::toResponse)
                                         .toList();
   }

   @Transactional
   public void deleteSchedule(UUID doctorId, UUID scheduleId) {
      UUID clinicId = ClinicContext.getClinicId();

      DoctorWorkingHours workingHours = doctorWorkingHoursRepository.findByDoctor_Clinic_IdAndIdAndDoctor_Id(clinicId,
                                                                                                             scheduleId,
                                                                                                             doctorId)
                                                                    .orElseThrow(() -> new ResourceNotFoundException(
                                                                            "This schedule for doctor not found or unavailable"));

      doctorWorkingHoursRepository.delete(workingHours);

      userAuditService.doctorWorkingHoursDeleted(workingHours.getId());
   }
}
