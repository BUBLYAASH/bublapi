package org.bublapi.dent.apikey.repository;

import org.bublapi.dent.apikey.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

   Optional<ApiKey> findByClinic_IdAndActiveTrue(UUID clinicId);

   Optional<ApiKey> findByPrefix(String prefix);

   boolean existsByClinic_IdAndActiveTrue(UUID clinicId);
}
