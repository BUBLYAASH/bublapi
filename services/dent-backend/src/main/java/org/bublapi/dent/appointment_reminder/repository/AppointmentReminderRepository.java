package org.bublapi.dent.appointment_reminder.repository;

import jakarta.persistence.LockModeType;
import org.bublapi.dent.appointment_reminder.entity.AppointmentReminder;
import org.bublapi.dent.appointment_reminder.entity.ReminderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, UUID> {
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("""
           SELECT ar FROM AppointmentReminder ar
           WHERE ar.status = :status AND ar.dueAt <= :dueTo
           """)
   List<AppointmentReminder> findDueForUpdate(@Param("status") ReminderStatus status,
                                              @Param("dueTo") LocalDateTime dueTo, Pageable pageable);

   @Query("""
           SELECT ar
           FROM AppointmentReminder ar
           WHERE ar.appointment.id = :appointmentId
           """)
   List<AppointmentReminder> findAllByAppointment_Id(@Param("appointmentId") UUID appointmentId);
}
