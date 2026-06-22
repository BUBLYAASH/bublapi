package org.bublapi.dent.patient.repository;

import org.bublapi.dent.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

   List<Patient> findAllByClinic_Id(UUID id);

   Optional<Patient> findByIdAndClinic_Id(UUID patientId, UUID clinicId);

   Optional<Patient> findByUser_Id(UUID userId);

   Optional<Patient> findByUser_IdAndClinic_Id(UUID userId, UUID clinicId);

   @Query("""
           SELECT p FROM Patient p WHERE p.clinic.id = :clinicId AND (p.email = :email OR p.phone = :phone)
           """)
   Optional<Patient> findByEmailOrPhoneInClinic(UUID clinicId, String email, String phone);
}
