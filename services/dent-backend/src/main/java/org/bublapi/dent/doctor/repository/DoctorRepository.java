package org.bublapi.dent.doctor.repository;

import jakarta.persistence.LockModeType;
import org.bublapi.dent.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
   @Query(value = """
           UPDATE doctors
           SET active = false, disabled_by_clinic = true
           WHERE clinic_id = :clinicId AND active = true
           """, nativeQuery = true)
   void disableAllByClinicId(@Param("clinicId") UUID clinicId);

   @Modifying
   @Query(value = """
           UPDATE doctors
           SET active = true, disabled_by_clinic = false
           WHERE clinic_id = :clinicId AND disabled_by_clinic = true
           """, nativeQuery = true)
   void enableAllDisabledByClinic(@Param("clinicId") UUID clinicId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("""
           SELECT d FROM Doctor d
           WHERE d.clinic.id = :clinicId
           AND d.id = :doctorId
           AND d.active = true
           """)
   Optional<Doctor> findActiveByIdForUpdate(@Param("clinicId") UUID clinicId, @Param("doctorId") UUID doctorId);
}
