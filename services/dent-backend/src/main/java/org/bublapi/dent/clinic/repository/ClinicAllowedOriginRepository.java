package org.bublapi.dent.clinic.repository;

import org.bublapi.dent.clinic.entity.ClinicAllowedOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicAllowedOriginRepository extends JpaRepository<ClinicAllowedOrigin, UUID> {
   List<ClinicAllowedOrigin> findAllByClinic_IdOrderByOrigin(UUID clinicId);

   boolean existsByClinic_IdAndClinic_ActiveTrueAndOrigin(UUID clinicId, String origin);

   boolean existsByClinic_ActiveTrueAndOrigin(String origin);

   boolean existsByClinic_IdAndOrigin(UUID clinicId, String origin);

   Optional<ClinicAllowedOrigin> findByIdAndClinic_Id(UUID id, UUID clinicId);

   Optional<ClinicAllowedOrigin> findByClinic_IdAndOrigin(UUID clinicId, String origin);
}
