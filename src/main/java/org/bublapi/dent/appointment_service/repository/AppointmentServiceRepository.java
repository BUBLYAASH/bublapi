package org.bublapi.dent.appointment_service.repository;

import org.bublapi.dent.appointment_service.entity.AppointmentServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentServiceRepository extends JpaRepository<AppointmentServiceItem, UUID> {
}
