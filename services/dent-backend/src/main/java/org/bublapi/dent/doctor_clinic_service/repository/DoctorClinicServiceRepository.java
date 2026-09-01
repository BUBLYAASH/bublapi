package org.bublapi.dent.doctor_clinic_service.repository;

import org.bublapi.dent.doctor_clinic_service.entity.DoctorClinicService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorClinicServiceRepository extends JpaRepository<DoctorClinicService, UUID> {
   List<DoctorClinicService> findAllByDoctor_Clinic_IdAndDoctor_Id(UUID clinicId, UUID doctorId);

   List<DoctorClinicService> findAllByDoctor_Clinic_IdAndClinicService_Id(UUID clinicId, UUID clinicServiceId);

   Optional<DoctorClinicService> findByDoctor_Clinic_IdAndDoctor_IdAndClinicService_Id(UUID clinicId, UUID doctorId,
                                                                                       UUID clinicServiceId);

   boolean existsByDoctor_Clinic_IdAndDoctor_IdAndClinicService_Id(UUID clinicId, UUID doctorId, UUID clinicServiceId);
}
