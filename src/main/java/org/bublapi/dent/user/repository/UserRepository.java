package org.bublapi.dent.user.repository;

import java.util.UUID;
import org.bublapi.dent.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

}
