package org.bublapi.dent.doctor_working_hours.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.doctor.entity.Doctor;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "doctor_working_hours", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"doctor_id", "day_of_week", "start_time", "end_time"})})
public class DoctorWorkingHours {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne
   @JoinColumn(name = "doctor_id", nullable = false)
   private Doctor doctor;

   @Enumerated(EnumType.STRING)
   @Column(name = "day_of_week", length = 10, nullable = false)
   private DayOfWeek dayOfWeek;

   @Column(name = "start_time", nullable = false)
   private LocalTime startTime;

   @Column(name = "end_time", nullable = false)
   private LocalTime endTime;
}
