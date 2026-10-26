package org.bublapi.dent.user.repository;

import org.bublapi.dent.role.entity.RoleName;
import org.bublapi.dent.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

   @Transactional
   @Modifying
   @Query("UPDATE User u SET u.lastLogin = :time WHERE u.id = :id AND (u.lastLogin IS NULL OR u.lastLogin < :time)")
   int recordLogin(@Param("id") UUID id, @Param("time") LocalDateTime time);

   List<User> findAllByClinic_IdAndEnabledTrue(UUID clinicId);

   List<User> findAllByClinic_Id(UUID clinicId);

   List<User> findAllByClinic_IdAndDisabledByClinicTrue(UUID clinicId);

   Optional<User> findByEmailIgnoreCaseAndClinic_Id(String email, UUID clinicId);

   @Query("""
           SELECT u FROM User u WHERE u.clinic.id = :clinicId AND (lower(u.email) = lower(:email) OR u.phone = :phone)
           """)
   Optional<User> findByEmailOrPhoneInClinic(@Param("email") String email, @Param("phone") String phone,
                                             @Param("clinicId") UUID clinicId);

   @Query("""
           SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.clinic.id = :clinicId AND lower(u.email) = lower(:email)
           """)
   Optional<User> findByEmailWithRolesInClinic(@Param("email") String email, @Param("clinicId") UUID clinicId);

   @Query("""
           SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id
           """)
   Optional<User> findByIdWithRoles(@Param("id") UUID id);

   Optional<User> findByIdAndClinic_Id(UUID id, UUID clinicId);

   @Query("""
           SELECT DISTINCT u FROM User u
           LEFT JOIN FETCH u.roles r
           WHERE lower(u.email) = lower(:email)
           AND u.clinic IS NULL
           AND r.name = :role
           """)
   Optional<User> findPlatformUserByEmailWithRole(@Param("email") String email,
                                                  @Param("role") RoleName role);

   default Optional<User> findAdminByEmailWithRoles(String email) {
      return findPlatformUserByEmailWithRole(email, RoleName.ADMIN);
   }

   @Modifying
   @Query("""
           UPDATE User AS u
           SET u.enabled = false, u.disabledByClinic = true
           WHERE u.clinic.id = :clinicId AND u.enabled = true
           """)
   void disableAllByClinicId(@Param("clinicId") UUID clinicId);

   @Modifying
   @Query("""
           UPDATE User AS u
           SET u.enabled = true, u.disabledByClinic = false
           WHERE u.clinic.id = :clinicId AND u.disabledByClinic = true
           """)
   void enableAllDisabledByClinic(@Param("clinicId") UUID clinicId);
}
