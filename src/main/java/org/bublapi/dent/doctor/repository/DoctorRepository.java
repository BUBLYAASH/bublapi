package org.bublapi.dent.doctor.repository;

import java.util.Optional;
import java.util.UUID;
import org.bublapi.dent.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

  Optional<Doctor> findByIdAndClinic_Id(UUID doctorId, UUID clinicId);
}
