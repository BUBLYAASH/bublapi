package org.bublapi.dent.appointment.repository;

import org.bublapi.dent.appointment.entity.Appointment;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

   Optional<Appointment> findByClinic_IdAndIdAndPatient_Id(UUID clinicId, UUID appointmentId, UUID patientId);

   Optional<Appointment> findByClinic_IdAndId(UUID clinicId, UUID appointmentId);

   @Query("""
           select (count(a) > 0)
           from Appointment a
           where a.clinic.id = :clinicId
             and a.doctor.id = :doctorId
             and a.status <> :cancelledStatus
             and a.scheduledAt < :endAt
             and a.endAt > :scheduledAt
           """)
   boolean existsOverlappingAppointment(@Param("clinicId") UUID clinicId, @Param("doctorId") UUID doctorId, @Param("scheduledAt") LocalDateTime scheduledAt, @Param("endAt") LocalDateTime endAt, @Param("cancelledStatus") AppointmentStatus cancelledStatus);

   List<Appointment> findAllByClinic_IdAndPatient_IdOrderByScheduledAtDesc(UUID clinicId, UUID patientId);

   List<Appointment> findAllByClinic_IdOrderByScheduledAtDesc(UUID clinicId);

   List<Appointment> findAllByClinic_IdAndDoctor_IdOrderByScheduledAtAsc(UUID clinicId, UUID doctorId);
}
