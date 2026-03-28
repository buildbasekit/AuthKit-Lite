package com.auth.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth.entities.RefreshToken;
import com.auth.entities.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByToken(String token);

	Optional<RefreshToken> findByUser(User user);
}
