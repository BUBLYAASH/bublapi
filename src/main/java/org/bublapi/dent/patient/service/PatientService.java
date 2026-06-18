package org.bublapi.dent.patient.service;

import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.patient.dto.CreatePatientRequestDto;
import org.bublapi.dent.patient.dto.PatientResponseDto;
import org.bublapi.dent.patient.dto.UpdatePatientRequestDto;
import org.bublapi.dent.patient.entity.Patient;
import org.bublapi.dent.patient.mapper.PatientMapper;
import org.bublapi.dent.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

  private final PatientRepository patientRepository;
  private final ClinicRepository clinicRepository;
  private final PatientMapper patientMapper;

  public PatientService(PatientRepository patientRepository, ClinicRepository clinicRepository,
      PatientMapper patientMapper) {
    this.patientRepository = patientRepository;
    this.clinicRepository = clinicRepository;
    this.patientMapper = patientMapper;
  }

  public PatientResponseDto create(UUID clinicId, CreatePatientRequestDto request) {
    Clinic clinic = clinicRepository.findById(clinicId)
        .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

    Patient patient = patientMapper.toEntity(request);
    patient.setClinic(clinic);

    Patient saved = patientRepository.save(patient);

    return patientMapper.toResponse(saved);
  }

  @Transactional
  public PatientResponseDto update(UUID clinicId, UUID patientId, UpdatePatientRequestDto request) {
    Patient patient = patientRepository.findByIdAndClinic_Id(patientId, clinicId)
        .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

    patientMapper.updateEntity(request, patient);

    return patientMapper.toResponse(patient);
  }
}
