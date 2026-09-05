package org.bublapi.dent.appointment_service.repository;

import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.appointment_service.entity.AppointmentServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AppointmentServiceRepository extends JpaRepository<AppointmentServiceItem, UUID> {
   List<AppointmentServiceItem> findAllByAppointment_Id(UUID appointmentId);

   @Query("""
           SELECT DISTINCT aps FROM AppointmentServiceItem aps
           WHERE aps.clinicService.id = :clinicServiceId
           AND aps.appointment.scheduledAt > :now 
           AND aps.appointment.status NOT IN :excludedStatuses
           """)
   List<AppointmentServiceItem> findAllAffectedByServiceDeactivation(@Param("clinicServiceId") UUID clinicServiceId,
                                                                     @Param("now") LocalDateTime now,
                                                                     @Param("excludedStatuses") Collection<AppointmentStatus> excludedStatuses);
}
