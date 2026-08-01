package org.bublapi.dent.patient.repository;

import org.bublapi.dent.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

   Optional<Patient> findByPhone(String phone);

   Optional<Patient> findByUser_Id(UUID userId);

   Optional<Patient> findByEmailIgnoreCaseOrPhone(String email, String phone);

   boolean existsByUser_Id(UUID userId);
}
