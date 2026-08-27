package org.bublapi.dent.clinic.repository;

import org.bublapi.dent.clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

   Optional<Clinic> findByIdAndActiveTrue(UUID id);

   List<Clinic> findAllByActiveTrue();
}
