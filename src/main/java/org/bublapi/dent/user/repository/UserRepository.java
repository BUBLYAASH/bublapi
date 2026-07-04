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

   List<User> findAllByClinic_Id(UUID clinicId);

   Optional<User> findByEmail(String email);

   Optional<User> findByEmailAndClinic_Id(String email, UUID clinicId);

   Optional<User> findByIdAndClinic_Id(UUID userId, UUID clinicId);

   @Query("""
           SELECT u FROM User u WHERE u.clinic.id = :clinicId AND (u.email = :email OR u.phone = :phone)
           """)
   Optional<User> findByEmailOrPhoneInClinic(UUID clinicId, String email, String phone);

   @Query("""
           SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email
           """)
   Optional<User> findByEmailWithRoles(@Param("email") String email);

   @Query("""
           SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id
           """)
   Optional<User> findByIdWithRoles(@Param("id") UUID id);
}
