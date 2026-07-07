package org.bublapi.dent.doctor_working_hours.repository;

import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorWorkingHoursRepository extends JpaRepository<DoctorWorkingHours, UUID> {
   Optional<DoctorWorkingHours> findByIdAndDoctor_Id(UUID id, UUID doctorId);

   List<DoctorWorkingHours> findAllByDoctor_Id(UUID doctorId);

   List<DoctorWorkingHours> findAllByDoctor_IdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek);
}
