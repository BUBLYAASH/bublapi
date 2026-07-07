package org.bublapi.dent.patient.repository;

import org.bublapi.dent.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

   List<Patient> findAllByClinic_Id(UUID id);

   Optional<Patient> findByPhone(String phone);

   Optional<Patient> findByUser_Id(UUID userId);

   Optional<Patient> findByEmailOrPhone(String email, String phone);

   boolean existsByUser_Id(UUID userId);
}
