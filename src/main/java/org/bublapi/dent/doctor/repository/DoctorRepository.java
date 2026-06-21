package org.bublapi.dent.doctor.repository;

import org.bublapi.dent.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

   List<Doctor> findAllByClinic_IdAndActiveTrue(UUID clinicId);

   @Query("""
           SELECT d FROM Doctor d WHERE d.id = :doctorId AND d.active = true AND d.clinic.id = :clinicId AND d.clinic.active = true
           """)
   Optional<Doctor> findAvailableDoctorInClinic(UUID clinicId, UUID doctorId);

   Optional<Doctor> findByIdAndClinic_Id(UUID doctorId, UUID clinicId);

   Optional<Doctor> findByUser_Id(UUID userId);
}
