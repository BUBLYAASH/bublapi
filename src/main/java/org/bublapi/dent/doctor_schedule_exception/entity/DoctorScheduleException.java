package org.bublapi.dent.doctor_schedule_exception.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bublapi.dent.doctor.entity.Doctor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "doctor_schedule_exceptions", uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "date", "start_time", "end_time"}))
public class DoctorScheduleException {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @ManyToOne
   @JoinColumn(name = "doctor_id", nullable = false)
   private Doctor doctor;

   @Column(nullable = false)
   private LocalDate date;

   @Enumerated(EnumType.STRING)
   @Column(length = 30, nullable = false)
   private ScheduleExceptionType type;

   @Column(name = "start_time")
   private LocalTime startTime;

   @Column(name = "end_time")
   private LocalTime endTime;

   private String reason;
}
