package org.bublapi.dent.appointment.repository;

import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

   Optional<Appointment> findByIdAndPatient_Id(UUID appointmentId, UUID patientId);

   @Query("""
           select (count(a) > 0)
           from Appointment a
           where a.doctor.id = :doctorId
             and a.status <> :cancelledStatus
             and a.scheduledAt < :endAt
             and a.endAt > :scheduledAt
           """)
   boolean existsOverlappingAppointment(@Param("doctorId") UUID doctorId, @Param("scheduledAt") LocalDateTime scheduledAt, @Param("endAt") LocalDateTime endAt, @Param("cancelledStatus") AppointmentStatus cancelledStatus);
}
