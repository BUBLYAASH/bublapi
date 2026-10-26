package org.bublapi.dent.user.repository;

import org.bublapi.dent.appointment.entity.AppointmentStatus;

import org.bublapi.dent.user.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserErasureRepository extends Repository<User, UUID> {
   @Query("SELECT COUNT(d) > 0 FROM Doctor d WHERE d.user.id = :userId")
   boolean hasDoctor(@Param("userId") UUID userId);

   @Modifying
   @Query("DELETE FROM Notification n WHERE n.user.id = :userId OR n.appointment.id IN (SELECT a.id FROM Appointment a WHERE a.patient.user.id = :userId)")
   void deleteNotifications(@Param("userId") UUID userId);

   @Query("SELECT a.id FROM Appointment a WHERE a.patient.user.id = :userId AND a.status <> :completed AND (a.status IN :planned OR a.scheduledAt >= :now)")
   List<UUID> findPlannedAppointments(@Param("userId") UUID userId, @Param("now") LocalDateTime now,
                                      @Param("completed") AppointmentStatus completed,
                                      @Param("planned") List<AppointmentStatus> planned);

   @Modifying
   @Query("DELETE FROM AppointmentServiceItem i WHERE i.appointment.id IN :ids")
   void deleteAppointmentItems(@Param("ids") List<UUID> ids);

   @Modifying
   @Query("DELETE FROM Appointment a WHERE a.id IN :ids")
   int deleteAppointments(@Param("ids") List<UUID> ids);

   @Modifying
   @Query("UPDATE Appointment a SET a.patient = NULL, a.comment = NULL WHERE a.patient.id IN (SELECT p.id FROM Patient p WHERE p.user.id = :userId)")
   int unlinkHistory(@Param("userId") UUID userId);

   @Modifying
   @Query("DELETE FROM Patient p WHERE p.user.id = :userId")
   void deletePatients(@Param("userId") UUID userId);

   @Modifying
   @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId")
   void deleteTokens(@Param("userId") UUID userId);
}
