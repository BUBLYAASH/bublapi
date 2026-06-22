package org.bublapi.dent.dental_service.repository;

import org.bublapi.dent.dental_service.entity.DentalService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DentalServiceRepository extends JpaRepository<DentalService, UUID> {

   boolean existsByTitle(String title);

   Optional<DentalService> findByIdAndActiveTrue(UUID id);

   List<DentalService> findAllByActiveTrue();
}
