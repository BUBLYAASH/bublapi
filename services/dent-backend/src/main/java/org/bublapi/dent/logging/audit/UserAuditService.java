package org.bublapi.dent.logging.audit;

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

      logAfterCommit(AuditAction.UPDATE, AuditEntityType.PATIENT, patientId, changedFields);
   }

   public void patientCreated(UUID patientId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.PATIENT, patientId, List.of());
   }

   public void appointmentCreated(UUID appointmentId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.APPOINTMENT, appointmentId, List.of());
   }

   public void appointmentCancelled(UUID appointmentId) {
      logAfterCommit(AuditAction.CANCEL, AuditEntityType.APPOINTMENT, appointmentId, List.of("status"));
   }

   public void appointmentStatusChanged(UUID appointmentId) {
      logAfterCommit(AuditAction.UPDATE, AuditEntityType.APPOINTMENT, appointmentId, List.of("status"));
   }

   public void doctorCreated(UUID doctorId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.DOCTOR, doctorId, List.of());
   }

   public void doctorUpdated(UUID doctorId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit(AuditAction.UPDATE, AuditEntityType.DOCTOR, doctorId, changedFields);
   }

   public void doctorDeactivated(UUID doctorId) {
      logAfterCommit(AuditAction.DEACTIVATE, AuditEntityType.DOCTOR, doctorId, List.of("active"));
   }

   public void doctorActivated(UUID doctorId) {
      logAfterCommit(AuditAction.ACTIVATE, AuditEntityType.DOCTOR, doctorId, List.of("active"));
   }

   public void clinicServiceCreated(UUID clinicServiceId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.CLINIC_SERVICE, clinicServiceId, List.of());
   }

   public void clinicServiceUpdated(UUID clinicServiceId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit(AuditAction.UPDATE, AuditEntityType.CLINIC_SERVICE, clinicServiceId, changedFields);
   }

   public void clinicServiceDeactivated(UUID clinicServiceId) {
      logAfterCommit(AuditAction.DEACTIVATE, AuditEntityType.CLINIC_SERVICE, clinicServiceId, List.of("active"));
   }

   public void clinicServiceActivated(UUID clinicServiceId) {
      logAfterCommit(AuditAction.ACTIVATE, AuditEntityType.CLINIC_SERVICE, clinicServiceId, List.of("active"));
   }

   public void doctorWorkingHoursCreated(UUID workingHoursId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.DOCTOR_WORKING_HOURS, workingHoursId, List.of());
   }

   public void doctorWorkingHoursUpdated(UUID workingHoursId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit(AuditAction.UPDATE, AuditEntityType.DOCTOR_WORKING_HOURS, workingHoursId, changedFields);
   }

   public void doctorWorkingHoursDeleted(UUID workingHoursId) {
      logAfterCommit(AuditAction.DELETE, AuditEntityType.DOCTOR_WORKING_HOURS, workingHoursId, List.of());
   }

   public void doctorScheduleExceptionCreated(UUID scheduleExceptionId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.DOCTOR_SCHEDULE_EXCEPTION, scheduleExceptionId, List.of());
   }

   public void doctorScheduleExceptionDeleted(UUID scheduleExceptionId) {
      logAfterCommit(AuditAction.DELETE, AuditEntityType.DOCTOR_SCHEDULE_EXCEPTION, scheduleExceptionId, List.of());
   }

   public void doctorClinicServiceCreated(UUID doctorClinicServiceId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.DOCTOR_CLINIC_SERVICE, doctorClinicServiceId, List.of());
   }

   public void doctorClinicServiceDeleted(UUID doctorClinicServiceId) {
      logAfterCommit(AuditAction.DELETE, AuditEntityType.DOCTOR_CLINIC_SERVICE, doctorClinicServiceId, List.of());
   }

   public void userUpdated(UUID userId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit(AuditAction.UPDATE, AuditEntityType.USER, userId, changedFields);
   }

   private void logAfterCommit(AuditAction action, AuditEntityType entityType, UUID entityId,
                               List<String> changedFields) {
      Runnable auditEvent = () -> {
         LoggingEventBuilder event = log.atInfo()
                                        .addKeyValue("action", action.name())
                                        .addKeyValue("entityType", entityType.name())
                                        .addKeyValue("entityId", entityId)
                                        .addKeyValue("result", "SUCCESS");

         if (changedFields != null && !changedFields.isEmpty()) {
            event.addKeyValue("changedFields", changedFields);
         }

         event.log(entityType + " " + action.name().toLowerCase());
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
