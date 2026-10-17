package com.tripnext.demo.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripnext.demo.model.RefreshToken;
import com.tripnext.demo.model.User;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Log the user out everywhere (password reset, account deletion, token reuse).
    @Modifying
    @Query("update RefreshToken t set t.isRevoked = true where t.user = :user and t.isRevoked = false")
    int revokeAllForUser(@Param("user") User user);
}
