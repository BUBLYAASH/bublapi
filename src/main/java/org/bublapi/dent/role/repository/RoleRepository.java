package org.bublapi.dent.role.repository;

import org.bublapi.dent.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

}
