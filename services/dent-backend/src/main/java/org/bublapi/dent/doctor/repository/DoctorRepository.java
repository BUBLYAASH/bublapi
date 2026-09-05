package org.bublapi.dent.doctor.repository;

import org.bublapi.dent.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

   List<Doctor> findAllByClinic_Id(UUID clinicId);

   List<Doctor> findAllByClinic_IdAndActiveTrue(UUID clinicId);

   List<Doctor> findAllByClinic_IdAndDisabledByClinicTrue(UUID clinicId);

   Optional<Doctor> findByClinic_IdAndIdAndActiveTrue(UUID clinicId, UUID doctorId);

   Optional<Doctor> findByClinic_IdAndUser_Id(UUID clinicId, UUID userId);

   Optional<Doctor> findByClinic_IdAndId(UUID clinicId, UUID doctorId);

   @Modifying
   @Query("""
           UPDATE Doctor AS d
           SET d.active = false, d.disabledByClinic = true
           WHERE d.clinic.id = :clinicId AND d.active = true
           """)
   void disableAllByClinicId(@Param("clinicId") UUID clinicId);

   @Modifying
   @Query("""
           UPDATE Doctor AS d
           SET d.active = true, d.disabledByClinic = false
           WHERE d.clinic.id = :clinicId AND d.disabledByClinic = true
           """)
   void enableAllDisabledByClinic(@Param("clinicId") UUID clinicId);
}
