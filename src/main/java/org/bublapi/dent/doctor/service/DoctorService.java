package org.bublapi.dent.doctor.service;

import jakarta.transaction.Transactional;
import java.util.UUID;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.doctor.dto.CreateDoctorRequestDto;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.dto.UpdateDoctorRequestDto;
import org.bublapi.dent.doctor.entity.Doctor;
import org.bublapi.dent.doctor.mapper.DoctorMapper;
import org.bublapi.dent.doctor.repository.DoctorRepository;
import org.springframework.stereotype.Service;

@Service
public class DoctorService {

  private final DoctorRepository doctorRepository;
  private final ClinicRepository clinicRepository;
  private final DoctorMapper doctorMapper;

  public DoctorService(DoctorRepository doctorRepository, ClinicRepository clinicRepository,
      DoctorMapper doctorMapper) {
    this.doctorRepository = doctorRepository;
    this.clinicRepository = clinicRepository;
    this.doctorMapper = doctorMapper;
  }

  public DoctorResponseDto create(UUID clinicId, CreateDoctorRequestDto request) {
    Clinic clinic = clinicRepository.findById(clinicId)
        .orElseThrow(() -> new RuntimeException("Clinic not found"));

    Doctor entity = doctorMapper.toEntity(request);
    entity.setClinic(clinic);
    Doctor saved = doctorRepository.save(entity);
    return doctorMapper.toResponse(saved);
  }

  @Transactional
  public DoctorResponseDto update(UUID clinicId, UUID doctorId, UpdateDoctorRequestDto request) {
    Doctor doctor = doctorRepository.findByIdAndClinic_Id(doctorId, clinicId)
            .orElseThrow(() -> new RuntimeException("Doctor in clinic not found"));

    doctorMapper.updateEntity(request, doctor);

    return doctorMapper.toResponse(doctor);
  }
}
