package org.bublapi.dent.clinic_service.repository;

import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicServiceRepository extends JpaRepository<ClinicService, UUID> {

   boolean existsByDentalService_Id(UUID dentalServiceId);

   Optional<ClinicService> findByIdAndActiveTrue(UUID clinicServiceId);

   List<ClinicService> findAllByActiveTrue();
}
