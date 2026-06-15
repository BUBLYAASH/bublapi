package org.bublapi.dent.clinic.repository;

import java.util.UUID;
import org.bublapi.dent.clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

}
