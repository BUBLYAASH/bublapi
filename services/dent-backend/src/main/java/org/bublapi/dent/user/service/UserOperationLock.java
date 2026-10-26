package org.bublapi.dent.user.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserOperationLock {
   private final EntityManager entityManager;

   public UserOperationLock(EntityManager entityManager) {
      this.entityManager = entityManager;
   }

   public void lockUser(UUID userId) {
      entityManager.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(CAST(:userId AS TEXT), 0))")
                   .setParameter("userId", userId).getSingleResult();
   }

   public void lockErasure(UUID userId) {
      entityManager.createNativeQuery("SET LOCAL lock_timeout = '5s'").executeUpdate();
      lockUser(userId);
      entityManager.createNativeQuery(
                           "LOCK TABLE users, patients, doctors, appointments, appointment_services, notifications, refresh_tokens, user_roles IN SHARE ROW EXCLUSIVE MODE")
                   .executeUpdate();
   }
}
