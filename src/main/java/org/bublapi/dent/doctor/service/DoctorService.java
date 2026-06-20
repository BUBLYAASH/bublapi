package org.bublapi.dent.doctor.service;

import java.util.List;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
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
        .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

    Doctor entity = doctorMapper.toEntity(request);
    entity.setClinic(clinic);
    Doctor saved = doctorRepository.save(entity);
    return doctorMapper.toResponse(saved);
  }

  @Transactional
  public DoctorResponseDto update(UUID clinicId, UUID doctorId, UpdateDoctorRequestDto request) {
    Doctor doctor = doctorRepository.findByIdAndClinic_Id(doctorId, clinicId)
        .orElseThrow(() -> new ResourceNotFoundException("Doctor in clinic not found"));

    doctorMapper.updateEntity(request, doctor);

    return doctorMapper.toResponse(doctor);
  }

  @Transactional
  public DoctorResponseDto deactivate(UUID clinicId, UUID doctorId) {
    Doctor doctor = doctorRepository.findByIdAndClinic_Id(doctorId, clinicId)
        .orElseThrow(() -> new ResourceNotFoundException("Doctor not found in this clinic"));

    doctor.setActive(false);

    return doctorMapper.toResponse(doctor);
  }

  @Transactional
  public DoctorResponseDto activate(UUID clinicId, UUID doctorId) {
    Doctor doctor = doctorRepository.findByIdAndClinic_Id(doctorId, clinicId)
        .orElseThrow(() -> new ResourceNotFoundException("Doctor not found in this clinic"));

    doctor.setActive(true);

    return doctorMapper.toResponse(doctor);
  }

  public List<DoctorResponseDto> findAll(UUID clinicId) {
    clinicRepository.findByIdAndActiveTrue(clinicId)
        .orElseThrow(() -> new ResourceNotFoundException("Clinic not found or unavailable"));

    return doctorRepository.findAllByClinic_IdAndActiveTrue(clinicId).stream()
        .map(doctorMapper::toResponse).toList();
  }
}
