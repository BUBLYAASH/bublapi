package org.bublapi.dent.doctor.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bublapi.dent.doctor.dto.DoctorResponseDto;
import org.bublapi.dent.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

  List<Doctor> findAllByClinic_IdAndActiveTrue(UUID clinicId);

  Optional<Doctor> findByIdAndClinic_Id(UUID doctorId, UUID clinicId);
}
