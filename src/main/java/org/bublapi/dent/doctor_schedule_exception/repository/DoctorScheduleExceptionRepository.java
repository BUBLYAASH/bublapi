package org.bublapi.dent.doctor_schedule_exception.repository;

import org.bublapi.dent.doctor_schedule_exception.entity.DoctorScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DoctorScheduleExceptionRepository extends JpaRepository<DoctorScheduleException, UUID> {

   Optional<DoctorScheduleException> findByIdAndDoctor_Id(UUID scheduleExceptionId, UUID doctorId);
}
