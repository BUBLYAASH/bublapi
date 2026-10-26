package org.bublapi.dent.user.service;

import jakarta.persistence.EntityManager;
import org.bublapi.dent.appointment.entity.AppointmentStatus;
import org.bublapi.dent.common.exception.BadRequestException;
import org.bublapi.dent.common.exception.ResourceNotFoundException;
import org.bublapi.dent.logging.audit.AdministrativeAuditService;
import org.bublapi.dent.user.dto.UserErasureRequestDto;
import org.bublapi.dent.user.dto.UserErasureResponseDto;
import org.bublapi.dent.user.repository.UserErasureRepository;
import org.bublapi.dent.user.repository.UserRepository;
import org.hibernate.Session;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class UserErasureService {
   private final UserRepository users;
   private final UserErasureRepository erasure;
   private final UserOperationLock operationLock;
   private final EntityManager entityManager;
   private final AdministrativeAuditService audit;

   public UserErasureService(UserRepository users, UserErasureRepository erasure, UserOperationLock operationLock,
                             EntityManager entityManager, AdministrativeAuditService audit) {
      this.users = users;
      this.erasure = erasure;
      this.operationLock = operationLock;
      this.entityManager = entityManager;
      this.audit = audit;
   }

   @PreAuthorize("hasRole('ADMIN')")
   @Transactional
   public UserErasureResponseDto erase(UUID userId, UserErasureRequestDto request) {
      if (!userId.equals(request.confirmationUserId()) || !request.retentionReviewed() || request.clinicId() == null) {
         throw new BadRequestException("Deletion confirmation and retention review are required");
      }
      operationLock.lockErasure(userId);
      var user = users.findByIdAndClinic_Id(userId, request.clinicId())
                      .orElseThrow(() -> new ResourceNotFoundException("Clinic user not found"));
      Session session = entityManager.unwrap(Session.class);
      boolean filtered = session.getEnabledFilter("clinicFilter") != null;
      session.disableFilter("clinicFilter");
      try {
         if (erasure.hasDoctor(userId)) {
            throw new BadRequestException("Linked doctor profile requires a separate retention review before deletion");
         }
         erasure.deleteNotifications(userId);
         var planned = erasure.findPlannedAppointments(userId, LocalDateTime.now(ZoneOffset.UTC),
                                                       AppointmentStatus.COMPLETED,
                                                       List.of(AppointmentStatus.CREATED, AppointmentStatus.CONFIRMED));
         int deleted = 0;
         if (!planned.isEmpty()) {
            erasure.deleteAppointmentItems(planned);
            deleted = erasure.deleteAppointments(planned);
         }
         int anonymized = erasure.unlinkHistory(userId);
         erasure.deletePatients(userId);
         erasure.deleteTokens(userId);
         users.delete(user);
         users.flush();
         audit.userErased(userId, request.clinicId());
         return new UserErasureResponseDto(deleted, anonymized,
                                           "Данные удалены из действующей базы. Остаточные данные в резервных копиях сохраняются до окончания срока их хранения. Восстановление старой копии может вернуть данные.");
      } finally {
         if (filtered) session.enableFilter("clinicFilter");
      }
   }
}
