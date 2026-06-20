package org.bublapi.dent.patient.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.bublapi.dent.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

  List<Patient> findAllByClinic_Id(UUID id);

  Optional<Patient> findByIdAndClinic_Id(UUID patientId, UUID clinicId);
}
