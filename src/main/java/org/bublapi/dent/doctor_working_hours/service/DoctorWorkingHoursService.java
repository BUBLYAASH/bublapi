package org.bublapi.dent.doctor_working_hours.service;

import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.bublapi.dent.doctor_working_hours.dto.DoctorWorkingHoursResponseDto;
import org.bublapi.dent.doctor_working_hours.dto.SetDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.dto.UpdateDoctorWorkingHoursRequestDto;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.bublapi.dent.doctor_working_hours.mapper.DoctorWorkingHoursMapper;
import org.bublapi.dent.doctor_working_hours.repository.DoctorWorkingHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DoctorWorkingHoursService {
   private final DoctorWorkingHoursRepository doctorWorkingHoursRepository;
   private final DoctorRepository doctorRepository;
   private final DoctorWorkingHoursMapper doctorWorkingHoursMapper;

   public DoctorWorkingHoursService(DoctorWorkingHoursRepository doctorWorkingHoursRepository, DoctorRepository doctorRepository, DoctorWorkingHoursMapper doctorWorkingHoursMapper) {
      this.doctorWorkingHoursRepository = doctorWorkingHoursRepository;
      this.doctorRepository = doctorRepository;
      this.doctorWorkingHoursMapper = doctorWorkingHoursMapper;
   }

   public DoctorWorkingHoursResponseDto setSchedule(UUID clinicId, UUID doctorId, SetDoctorWorkingHoursRequestDto request) {
      Doctor doctor = doctorRepository.findAvailableDoctorInClinic(clinicId, doctorId)
                                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      DoctorWorkingHours workingHours = doctorWorkingHoursMapper.toEntity(request);

      workingHours.setDoctor(doctor);

      DoctorWorkingHours saved = doctorWorkingHoursRepository.save(workingHours);

      return doctorWorkingHoursMapper.toResponse(saved);
   }

   @Transactional
   public DoctorWorkingHoursResponseDto updateSchedule(UUID clinicId, UUID doctorId, UUID scheduleId, UpdateDoctorWorkingHoursRequestDto request) {
      doctorRepository.findAvailableDoctorInClinic(clinicId, doctorId)
                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      DoctorWorkingHours workingHours = doctorWorkingHoursRepository.findByIdAndDoctor_Id(scheduleId, doctorId)
                                                                    .orElseThrow(() -> new ResourceNotFoundException("This schedule for doctor not found or unavailable"));

      doctorWorkingHoursMapper.updateEntity(request, workingHours);

      return doctorWorkingHoursMapper.toResponse(workingHours);
   }

   public List<DoctorWorkingHoursResponseDto> getSchedule(UUID clinicId, UUID doctorId) {
      doctorRepository.findAvailableDoctorInClinic(clinicId, doctorId)
                      .orElseThrow(() -> new ResourceNotFoundException("Doctor not found or unavailable"));

      return doctorWorkingHoursRepository.findAllByDoctor_Id(doctorId)
                                         .stream()
                                         .map(doctorWorkingHoursMapper::toResponse)
                                         .toList();
   }

   public void deleteSchedule(UUID clinicId, UUID doctorId, UUID scheduleId) {
      DoctorWorkingHours workingHours = doctorWorkingHoursRepository.findByIdAndDoctor_IdAndDoctor_Clinic_Id(scheduleId, doctorId, clinicId)
                                                                    .orElseThrow(() -> new ResourceNotFoundException("This schedule for doctor not found or unavailable"));

      doctorWorkingHoursRepository.delete(workingHours);
   }
}
