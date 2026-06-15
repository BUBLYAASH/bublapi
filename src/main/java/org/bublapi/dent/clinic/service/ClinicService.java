package org.bublapi.dent.clinic.service;

import java.util.UUID;
import org.bublapi.dent.clinic.dto.ClinicResponseDto;
import org.bublapi.dent.clinic.dto.CreateClinicRequestDto;
import org.bublapi.dent.clinic.dto.UpdateClinicRequestDto;
import org.bublapi.dent.clinic.entity.Clinic;
import org.bublapi.dent.clinic.mapper.ClinicMapper;
import org.bublapi.dent.clinic.repository.ClinicRepository;
import org.springframework.stereotype.Service;

@Service
public class ClinicService {
  private final ClinicRepository clinicRepository;
  private final ClinicMapper clinicMapper;

  public ClinicService(ClinicRepository clinicRepository, ClinicMapper clinicMapper) {
    this.clinicRepository = clinicRepository;
    this.clinicMapper = clinicMapper;
  }

  public ClinicResponseDto create(CreateClinicRequestDto request){
    Clinic clinic = clinicMapper.toEntity(request);

    Clinic saved = clinicRepository.save(clinic);

    return clinicMapper.toResponse(saved);
  }

  public ClinicResponseDto update(UUID id, UpdateClinicRequestDto request) {
    Clinic clinic = clinicRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Clinic not found"));

    clinicMapper.updateEntity(request, clinic);

    Clinic saved =  clinicRepository.save(clinic);

    return clinicMapper.toResponse(saved);
  }
}
