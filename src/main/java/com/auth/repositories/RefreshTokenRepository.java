package com.auth.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth.entities.RefreshToken;
import com.auth.entities.User;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	Optional<RefreshToken> findByUser(User user);
	
	@Modifying
	@Query("DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
	int deleteByTokenHash(String tokenHash);
}
