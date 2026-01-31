package com.realestate.auth.repository;

import com.realestate.auth.entity.RsaKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RsaKeyRepository extends JpaRepository<RsaKey, String> {
    Optional<RsaKey> findFirstByActiveTrue();

    List<RsaKey> findByActiveTrue();

    List<RsaKey> findByActiveTrueOrExpiresAtAfter(LocalDateTime date);

    List<RsaKey> findByActiveTrueAndExpiresAtBefore(LocalDateTime date);

    @Modifying
    @Query("DELETE FROM RsaKey r WHERE r.active = false AND r.expiresAt < :date")
    int deleteByActiveFalseAndExpiresAtBefore(@Param("date") LocalDateTime date);
}
