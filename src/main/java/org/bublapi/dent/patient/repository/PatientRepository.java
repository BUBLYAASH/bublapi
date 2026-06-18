package org.bublapi.dent.patient.repository;

import java.util.Optional;
import java.util.UUID;
import org.bublapi.dent.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

  Optional<Patient> findByIdAndClinic_Id(UUID patientId, UUID clinicId);
}
