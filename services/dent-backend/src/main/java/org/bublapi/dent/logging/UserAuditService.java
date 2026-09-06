package org.bublapi.dent.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class UserAuditService {

   private final Logger log = LoggerFactory.getLogger("USER_AUDIT");

   public void patientUpdated(UUID patientId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit("UPDATE", "PATIENT", patientId, changedFields);
   }

   public void patientCreated(UUID patientId) {
      logAfterCommit("CREATE", "PATIENT", patientId, List.of());
   }

   public void appointmentCreated(UUID appointmentId) {
      logAfterCommit("CREATE", "APPOINTMENT", appointmentId, List.of());
   }

   public void appointmentCancelled(UUID appointmentId) {
      logAfterCommit("CANCEL", "APPOINTMENT", appointmentId, List.of("status"));
   }

   public void appointmentStatusChanged(UUID appointmentId) {
      logAfterCommit("UPDATE", "APPOINTMENT", appointmentId, List.of("status"));
   }

   public void doctorCreated(UUID doctorId) {
      logAfterCommit("CREATE", "DOCTOR", doctorId, List.of());
   }

   public void doctorUpdated(UUID doctorId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit("UPDATE", "DOCTOR", doctorId, changedFields);
   }

   public void doctorDeactivated(UUID doctorId) {
      logAfterCommit("DEACTIVATE", "DOCTOR", doctorId, List.of("active"));
   }

   public void doctorActivated(UUID doctorId) {
      logAfterCommit("ACTIVATE", "DOCTOR", doctorId, List.of("active"));
   }

   public void clinicServiceCreated(UUID clinicServiceId) {
      logAfterCommit("CREATE", "CLINIC_SERVICE", clinicServiceId, List.of());
   }

   public void clinicServiceUpdated(UUID clinicServiceId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit("UPDATE", "CLINIC_SERVICE", clinicServiceId, changedFields);
   }

   public void clinicServiceDeactivated(UUID clinicServiceId) {
      logAfterCommit("DEACTIVATE", "CLINIC_SERVICE", clinicServiceId, List.of("active"));
   }

   public void clinicServiceActivated(UUID clinicServiceId) {
      logAfterCommit("ACTIVATE", "CLINIC_SERVICE", clinicServiceId, List.of("active"));
   }

   public void doctorWorkingHoursCreated(UUID workingHoursId) {
      logAfterCommit("CREATE", "DOCTOR_WORKING_HOURS", workingHoursId, List.of());
   }

   public void doctorWorkingHoursUpdated(UUID workingHoursId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit("UPDATE", "DOCTOR_WORKING_HOURS", workingHoursId, changedFields);
   }

   public void doctorWorkingHoursDeleted(UUID workingHoursId) {
      logAfterCommit("DELETE", "DOCTOR_WORKING_HOURS", workingHoursId, List.of());
   }

   public void doctorScheduleExceptionCreated(UUID scheduleExceptionId) {
      logAfterCommit("CREATE", "DOCTOR_SCHEDULE_EXCEPTION", scheduleExceptionId, List.of());
   }

   public void doctorScheduleExceptionDeleted(UUID scheduleExceptionId) {
      logAfterCommit("DELETE", "DOCTOR_SCHEDULE_EXCEPTION", scheduleExceptionId, List.of());
   }

   public void doctorClinicServiceCreated(UUID doctorClinicServiceId) {
      logAfterCommit("CREATE", "DOCTOR_CLINIC_SERVICE", doctorClinicServiceId, List.of());
   }

   public void doctorClinicServiceDeleted(UUID doctorClinicServiceId) {
      logAfterCommit("DELETE", "DOCTOR_CLINIC_SERVICE", doctorClinicServiceId, List.of());
   }

   private void logAfterCommit(String action, String entityType, UUID entityId, List<String> changedFields) {
      Runnable auditEvent = () -> {
         LoggingEventBuilder event = log.atInfo()
                                        .addKeyValue("action", action)
                                        .addKeyValue("entityType", entityType)
                                        .addKeyValue("entityId", entityId)
                                        .addKeyValue("result", "SUCCESS");

         if (changedFields != null && !changedFields.isEmpty()) {
            event.addKeyValue("changedFields", changedFields);
         }

         event.log(entityType + " " + action.toLowerCase());
      };

      if (TransactionSynchronizationManager.isSynchronizationActive()) {
         TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
               auditEvent.run();
            }
         });
      } else {
         auditEvent.run();
      }
   }
}
