package org.bublapi.dent.doctor_clinic_service.repository;

import org.bublapi.dent.doctor_clinic_service.entity.DoctorClinicService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorClinicServiceRepository extends JpaRepository<DoctorClinicService, UUID> {
   List<DoctorClinicService> findAllByDoctor_Id(UUID doctorId);

   List<DoctorClinicService> findAllByClinicService_Id(UUID clinicServiceId);

   Optional<DoctorClinicService> findByDoctor_IdAndClinicService_Id(UUID doctorId, UUID clinicServiceId);

   boolean existsByDoctor_IdAndClinicService_Id(UUID doctorId, UUID clinicServiceId);
}
