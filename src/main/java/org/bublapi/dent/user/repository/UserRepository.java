package org.bublapi.dent.user.repository;

import org.bublapi.dent.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

   List<User> findAllByClinic_IdAndEnabledTrue(UUID clinicId);

   List<User> findAllByClinic_IdAndDisabledByClinicTrue(UUID clinicId);

   Optional<User> findByEmailIgnoreCaseAndClinic_Id(String email, UUID clinicId);

   @Query("""
           SELECT u FROM User u WHERE u.clinic.id = :clinicId AND (lower(u.email) = lower(:email) OR u.phone = :phone)
           """)
   Optional<User> findByEmailOrPhoneInClinic(@Param("email") String email, @Param("phone") String phone, @Param("clinicId") UUID clinicId);

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
           AND r.name = org.bublapi.dent.role.entity.RoleName.ADMIN
           """)
   Optional<User> findAdminByEmailWithRoles(@Param("email") String email);
}
