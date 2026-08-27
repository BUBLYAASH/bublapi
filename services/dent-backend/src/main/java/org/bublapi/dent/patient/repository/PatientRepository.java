package org.bublapi.dent.patient.repository;

import org.bublapi.dent.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

   Optional<Patient> findByClinic_IdAndPhone(UUID clinicId, String phone);

   Optional<Patient> findByClinic_IdAndUser_Id(UUID clinicId, UUID userId);

   @Query("""
           SELECT p FROM Patient p WHERE p.clinic.id = :clinicId AND (lower(p.email) = :email OR p.phone = :phone)
           """)
   Optional<Patient> findByClinic_IdAndEmailIgnoreCaseOrPhone(@Param("clinicId") UUID clinicId, @Param("email") String email, @Param("phone") String phone);

   Optional<Patient> findByClinic_IdAndId(UUID clinicId, UUID patientId);

   List<Patient> findAllByClinic_Id(UUID clinicId);

   boolean existsByClinic_IdAndUser_Id(UUID clinicId, UUID userId);

   boolean existsByClinic_IdAndId(UUID clinicId, UUID patientId);
}
