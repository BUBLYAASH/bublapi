package org.bublapi.dent.doctor_clinic_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.clinic_service.entity.ClinicService;
import org.bublapi.dent.doctor.entity.Doctor;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "doctor_clinic_services", uniqueConstraints = @UniqueConstraint(name = "uk_doctor_clinic_service", columnNames = {
        "doctor_id", "clinic_service_id"}))
public class DoctorClinicService {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   @JoinColumn(name = "doctor_id", nullable = false)
   private Doctor doctor;

   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   @JoinColumn(name = "clinic_service_id", nullable = false)
   private ClinicService clinicService;
}
