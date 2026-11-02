package org.bublapi.dent.apikey.repository;

import org.bublapi.dent.apikey.entity.ApiKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

   Optional<ApiKey> findByClinic_IdAndActiveTrue(UUID clinicId);

   @EntityGraph(attributePaths = "clinic")
   Optional<ApiKey> findByPrefix(String prefix);

   boolean existsByClinic_IdAndActiveTrue(UUID clinicId);

   @Query("""
           SELECT ak.clinic.id FROM ApiKey ak
           WHERE ak.graceUntil < :now
           AND ak.active = true
           AND ak.clinic.active = true
           """)
   List<UUID> findAllExpiredClinicIds(@Param("now") LocalDateTime now);
}
