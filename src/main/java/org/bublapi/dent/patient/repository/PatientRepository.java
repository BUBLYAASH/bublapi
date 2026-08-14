package org.bublapi.dent.patient.repository;

import org.bublapi.dent.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

   Optional<Patient> findByClinic_IdAndPhone(UUID clinicId, String phone);

   Optional<Patient> findByClinic_IdAndUser_Id(UUID clinicId, UUID userId);

   Optional<Patient> findByClinic_IdAndEmailIgnoreCaseOrPhone(UUID clinicId, String email, String phone);

   Optional<Patient> findByClinic_IdAndId(UUID clinicId, UUID patientId);

   List<Patient> findAllByClinic_Id(UUID clinicId);

   boolean existsByClinic_IdAndUser_Id(UUID clinicId, UUID userId);

   boolean existsByClinic_IdAndId(UUID clinicId, UUID patientId);
}
