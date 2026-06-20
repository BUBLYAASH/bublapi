package org.bublapi.dent.user.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.bublapi.dent.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  List<User> findAllByClinic_IdAndEnabledTrue(UUID clinicId);

  List<User> findAllByClinic_Id(UUID clinicId);

  Optional<User> findByIdAndClinic_Id(UUID userId, UUID clinicId);
}
