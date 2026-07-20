package org.bublapi.dent.doctor_working_hours.repository;

import org.bublapi.dent.doctor_working_hours.entity.DayOfWeek;
import org.bublapi.dent.doctor_working_hours.entity.DoctorWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorWorkingHoursRepository extends JpaRepository<DoctorWorkingHours, UUID> {
   Optional<DoctorWorkingHours> findByIdAndDoctor_Id(UUID id, UUID doctorId);

   List<DoctorWorkingHours> findAllByDoctor_Id(UUID doctorId);

   List<DoctorWorkingHours> findAllByDoctor_IdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek);

   @Query("""
           SELECT count(wh) > 0 FROM DoctorWorkingHours wh
           WHERE wh.doctor.id = :doctorId
           AND wh.dayOfWeek = :dayOfWeek
           AND wh.startTime < :endTime
           AND wh.endTime > :startTime
           """)
   boolean existsOverlappingInterval(@Param("doctorId") UUID doctorId, @Param("dayOfWeek") DayOfWeek dayOfWeek, @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

   @Query("""
           SELECT count(wh) > 0 FROM DoctorWorkingHours wh
           WHERE wh.doctor.id = :doctorId
           AND wh.id <> :scheduleId
           AND wh.dayOfWeek = :dayOfWeek
           AND wh.startTime < :endTime
           AND wh.endTime > :startTime
           """)
   boolean existsOverlappingIntervalExcept(@Param("doctorId") UUID doctorId, @Param("scheduleId") UUID scheduleId, @Param("dayOfWeek") DayOfWeek dayOfWeek, @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);
}
