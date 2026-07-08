package org.bublapi.dent.user.repository;

import org.bublapi.dent.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

   List<User> findAllByClinic_IdAndEnabledTrue(UUID clinicId);

   List<User> findAllByClinic_IdAndDisabledByClinicTrue(UUID clinicId);

   Optional<User> findByEmailAndClinic_Id(String email, UUID clinicId);

   @Query("""
           SELECT u FROM User u WHERE u.clinic.id = :clinicId AND (u.email = :email OR u.phone = :phone)
           """)
   Optional<User> findByEmailOrPhoneInClinic(@Param("email") String email, @Param("phone") String phone, @Param("clinicId") UUID clinicId);

   @Query("""
           SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.clinic.id = :clinicId AND u.email = :email
           """)
   Optional<User> findByEmailWithRolesInClinic(@Param("email") String email, @Param("clinicId") UUID clinicId);

   @Query("""
           SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id
           """)
   Optional<User> findByIdWithRoles(@Param("id") UUID id);

   Optional<User> findByIdAndClinicId(UUID id, UUID clinicId);
}
