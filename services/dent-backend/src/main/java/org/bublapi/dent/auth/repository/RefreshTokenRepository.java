package org.bublapi.dent.auth.repository;

import jakarta.persistence.LockModeType;
import org.bublapi.dent.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("""
           SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash
           """)
   Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
