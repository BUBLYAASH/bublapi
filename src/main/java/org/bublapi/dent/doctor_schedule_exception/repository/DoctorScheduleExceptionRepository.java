package org.bublapi.dent.doctor_schedule_exception.repository;

import org.bublapi.dent.doctor_schedule_exception.entity.DoctorScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorScheduleExceptionRepository extends JpaRepository<DoctorScheduleException, UUID> {

   Optional<DoctorScheduleException> findByIdAndDoctor_Id(UUID scheduleExceptionId, UUID doctorId);

   List<DoctorScheduleException> findAllByDoctor_IdAndDate(UUID doctorId, LocalDate date);
}
