package org.bublapi.dent.role.repository;

import java.util.UUID;
import org.bublapi.dent.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

}
