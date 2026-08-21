package org.bublapi.dent.clinic_service.repository;

import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicServiceRepository extends JpaRepository<ClinicService, UUID> {

   boolean existsByClinic_IdAndDentalService_Id(UUID clinicId, UUID dentalServiceId);

   Optional<ClinicService> findByClinic_IdAndIdAndActiveTrue(UUID clinicId, UUID clinicServiceId);

   Optional<ClinicService> findByClinic_IdAndId(UUID clinicId, UUID clinicServiceId);

   List<ClinicService> findAllByClinic_IdAndActiveTrue(UUID clinicId);

   List<ClinicService> findAllByClinic_Id(UUID clinicId);

   List<ClinicService> findAllByClinic_IdAndDisabledByClinicTrue(UUID clinicId);
}
