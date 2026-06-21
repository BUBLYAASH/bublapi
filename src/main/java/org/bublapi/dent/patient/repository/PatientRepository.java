package org.bublapi.dent.patient.repository;

import org.bublapi.dent.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

   List<Patient> findAllByClinic_Id(UUID id);

   Optional<Patient> findByIdAndClinic_Id(UUID patientId, UUID clinicId);

   Optional<Patient> findByUser_Id(UUID userId);

   Optional<Patient> findByUser_IdAndClinic_Id(UUID userId, UUID clinicId);
}
