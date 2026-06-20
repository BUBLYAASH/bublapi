package org.bublapi.dent.doctor.repository;

import org.bublapi.dent.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    List<Doctor> findAllByClinic_IdAndActiveTrue(UUID clinicId);

    Optional<Doctor> findByIdAndClinic_Id(UUID doctorId, UUID clinicId);
}
