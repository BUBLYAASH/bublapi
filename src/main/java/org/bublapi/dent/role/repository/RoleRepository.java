package org.bublapi.dent.role.repository;

import org.bublapi.dent.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

   Optional<Role> findByName(String name);
}
