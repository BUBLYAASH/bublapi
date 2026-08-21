package org.bublapi.dent.doctor.repository;

import org.bublapi.dent.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
