package org.bublapi.dent.logging.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class AdministrativeAuditService {

   private static final Logger log = LoggerFactory.getLogger("ADMIN_AUDIT");

   public void apiKeyCreated(UUID apiKeyId, UUID clinicId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.API_KEY, apiKeyId, clinicId);
   }

   public void apiKeyRenewed(UUID apiKeyId, UUID clinicId) {
      logAfterCommit(AuditAction.RENEW, AuditEntityType.API_KEY, apiKeyId, clinicId);
   }

   public void apiKeyRotated(UUID apiKeyId, UUID clinicId) {
      logAfterCommit(AuditAction.ROTATE, AuditEntityType.API_KEY, apiKeyId, clinicId);
   }

   public void apiKeyRevoked(UUID apiKeyId, UUID clinicId) {
      logAfterCommit(AuditAction.REVOKE, AuditEntityType.API_KEY, apiKeyId, clinicId);
   }

   public void roleGranted(UUID targetUserId, UUID clinicId, String role) {
      logAfterCommit(AuditAction.GRANT_ROLE, AuditEntityType.USER, targetUserId, clinicId, role);
   }

   public void roleRevoked(UUID targetUserId, UUID clinicId, String role) {
      logAfterCommit(AuditAction.REVOKE_ROLE, AuditEntityType.USER, targetUserId, clinicId, role);
   }

   public void userDeactivated(UUID targetUserId, UUID clinicId) {
      logAfterCommit(AuditAction.DEACTIVATE, AuditEntityType.USER, targetUserId, clinicId);
   }

   public void userActivated(UUID targetUserId, UUID clinicId) {
      logAfterCommit(AuditAction.ACTIVATE, AuditEntityType.USER, targetUserId, clinicId);
   }

   public void clinicCreated(UUID clinicId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.CLINIC, clinicId, clinicId);
   }

   public void clinicUpdated(UUID clinicId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit(AuditAction.UPDATE, AuditEntityType.CLINIC, clinicId, clinicId, changedFields);
   }

   public void clinicDeactivated(UUID clinicId) {
      logAfterCommit(AuditAction.DEACTIVATE, AuditEntityType.CLINIC, clinicId, clinicId);
   }

   public void clinicActivated(UUID clinicId) {
      logAfterCommit(AuditAction.ACTIVATE, AuditEntityType.CLINIC, clinicId, clinicId);
   }

   public void dentalServiceCreated(UUID dentalServiceId) {
      logAfterCommit(AuditAction.CREATE, AuditEntityType.DENTAL_SERVICE, dentalServiceId, null);
   }

   public void dentalServiceUpdated(UUID dentalServiceId, List<String> changedFields) {
      if (changedFields == null || changedFields.isEmpty()) {
         return;
      }

      logAfterCommit(AuditAction.UPDATE, AuditEntityType.DENTAL_SERVICE, dentalServiceId, null, changedFields);
   }

   public void dentalServiceDeactivated(UUID dentalServiceId) {
      logAfterCommit(AuditAction.DEACTIVATE, AuditEntityType.DENTAL_SERVICE, dentalServiceId, null);
   }

   public void dentalServiceActivated(UUID dentalServiceId) {
      logAfterCommit(AuditAction.ACTIVATE, AuditEntityType.DENTAL_SERVICE, dentalServiceId, null);
   }

   public void notificationRetried(UUID notificationId, UUID clinicId) {
      logAfterCommit(AuditAction.RETRY, AuditEntityType.NOTIFICATION, notificationId, clinicId);
   }

   public void notificationDeleted(UUID notificationId, UUID clinicId) {
      logAfterCommit(AuditAction.DELETE, AuditEntityType.NOTIFICATION, notificationId, clinicId);
   }

   private void logAfterCommit(AuditAction action, AuditEntityType entityType, UUID entityId, UUID clinicId,
                               String role, List<String> changedFields) {
      Runnable auditEvent = () -> {
         LoggingEventBuilder event = log.atInfo()
                                        .addKeyValue("action", action.name())
                                        .addKeyValue("entityType", entityType.name())
                                        .addKeyValue("entityId", entityId)
                                        .addKeyValue("result", "SUCCESS");

         if (clinicId != null && MDC.get("clinicId") == null) {
            event.addKeyValue("clinicId", clinicId);
         }

         if (role != null) {
            event.addKeyValue("role", role);
         }

         if (changedFields != null && !changedFields.isEmpty()) {
            event.addKeyValue("changedFields", changedFields);
         }

         event.log("Administrative action completed");
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

   private void logAfterCommit(AuditAction action, AuditEntityType entityType, UUID entityId, UUID clinicId) {

      logAfterCommit(action, entityType, entityId, clinicId, null, null);
   }

   private void logAfterCommit(AuditAction action, AuditEntityType entityType, UUID entityId, UUID clinicId,
                               String role) {

      logAfterCommit(action, entityType, entityId, clinicId, role, null);
   }

   private void logAfterCommit(AuditAction action, AuditEntityType entityType, UUID entityId, UUID clinicId,
                               List<String> changedFields) {

      logAfterCommit(action, entityType, entityId, clinicId, null, changedFields);
   }
}
