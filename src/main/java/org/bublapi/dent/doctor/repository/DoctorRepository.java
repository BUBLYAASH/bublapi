package org.bublapi.dent.doctor.repository;

import org.bublapi.dent.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

   List<Doctor> findAllByActiveTrue();

   Optional<Doctor> findByIdAndActiveTrue(UUID doctorId);

   Optional<Doctor> findByUser_Id(UUID userId);
}
