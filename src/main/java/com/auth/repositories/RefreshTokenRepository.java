package com.auth.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.auth.entities.RefreshToken;
import com.auth.entities.User;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM RefreshToken r JOIN FETCH r.user WHERE r.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

	@Modifying
	@Query("DELETE FROM RefreshToken r WHERE r.user = :user")
	int deleteByUser(User user);

	@Modifying
	@Query("DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
	int deleteByTokenHash(String tokenHash);
}
