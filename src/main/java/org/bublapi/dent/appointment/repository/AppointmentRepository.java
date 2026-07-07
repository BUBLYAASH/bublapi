package org.bublapi.dent.appointment.repository;

import org.bublapi.dent.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

   Optional<Appointment> findByIdAndPatient_Id(UUID appointmentId, UUID patientId);
}
