package org.bublapi.dent.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class AdministrativeAuditService {

   private static final Logger log = LoggerFactory.getLogger("ADMIN_AUDIT");

   public void apiKeyCreated(UUID apiKeyId, UUID clinicId) {
      logAfterCommit("CREATE", "API_KEY", apiKeyId, clinicId);
   }

   public void apiKeyRenewed(UUID apiKeyId, UUID clinicId) {
      logAfterCommit("RENEW", "API_KEY", apiKeyId, clinicId);
   }

   public void apiKeyRotated(UUID apiKeyId, UUID clinicId) {
      logAfterCommit("ROTATE", "API_KEY", apiKeyId, clinicId);
   }

   public void apiKeyRevoked(UUID apiKeyId, UUID clinicId) {
      logAfterCommit("REVOKE", "API_KEY", apiKeyId, clinicId);
   }

   public void roleGranted(UUID targetUserId, UUID clinicId, String role) {
      logAfterCommit("GRANT_ROLE", "USER", targetUserId, clinicId, role);
   }

   public void roleRevoked(UUID targetUserId, UUID clinicId, String role) {
      logAfterCommit("REVOKE_ROLE", "USER", targetUserId, clinicId, role);
   }

   public void userDeactivated(UUID targetUserId, UUID clinicId) {
      logAfterCommit("DEACTIVATE", "USER", targetUserId, clinicId);
   }

   public void userActivated(UUID targetUserId, UUID clinicId) {
      logAfterCommit("ACTIVATE", "USER", targetUserId, clinicId);
   }

   public void clinicCreated(UUID clinicId) {
      logAfterCommit("CREATE", "CLINIC", clinicId, clinicId);
   }

   public void clinicUpdated(UUID clinicId) {
      logAfterCommit("UPDATE", "CLINIC", clinicId, clinicId);
   }

   public void clinicDeactivated(UUID clinicId) {
      logAfterCommit("DEACTIVATE", "CLINIC", clinicId, clinicId);
   }

   public void clinicActivated(UUID clinicId) {
      logAfterCommit("ACTIVATE", "CLINIC", clinicId, clinicId);
   }

   public void dentalServiceCreated(UUID dentalServiceId) {
      logAfterCommit("CREATE", "DENTAL_SERVICE", dentalServiceId, null);
   }

   public void dentalServiceUpdated(UUID dentalServiceId) {
      logAfterCommit("UPDATE", "DENTAL_SERVICE", dentalServiceId, null);
   }

   public void dentalServiceDeactivated(UUID dentalServiceId) {
      logAfterCommit("DEACTIVATE", "DENTAL_SERVICE", dentalServiceId, null);
   }

   public void dentalServiceActivated(UUID dentalServiceId) {
      logAfterCommit("ACTIVATE", "DENTAL_SERVICE", dentalServiceId, null);
   }

   private void logAfterCommit(String action, String entityType, UUID entityId, UUID clinicId) {
      Runnable auditEvent = () -> {
         LoggingEventBuilder event = log.atInfo()
                                        .addKeyValue("action", action)
                                        .addKeyValue("entityType", entityType)
                                        .addKeyValue("entityId", entityId)
                                        .addKeyValue("result", "SUCCESS");

         if (clinicId != null && MDC.get("clinicId") == null) {
            event.addKeyValue("clinicId", clinicId);
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

   private void logAfterCommit(String action, String entityType, UUID entityId, UUID clinicId, String role) {
      Runnable auditEvent = () -> {
         LoggingEventBuilder event = log.atInfo()
                                        .addKeyValue("action", action)
                                        .addKeyValue("entityType", entityType)
                                        .addKeyValue("entityId", entityId)
                                        .addKeyValue("result", "SUCCESS")
                                        .addKeyValue("role", role);

         if (clinicId != null && MDC.get("clinicId") == null) {
            event.addKeyValue("clinicId", clinicId);
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
}
