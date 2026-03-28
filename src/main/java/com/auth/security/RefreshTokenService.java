package com.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth.entities.RefreshToken;
import com.auth.entities.User;
import com.auth.exceptions.RefreshTokenException;
import com.auth.repositories.RefreshTokenRepository;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {
	private final RefreshTokenRepository refreshTokenRepository;

	@Value("${app.jwt.refresh-token-expiration-ms}")
	private Long refreshDurationMs;

	private final JwtUtils jwtUtils;

	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtils jwtUtils) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtUtils = jwtUtils;
	}

	public RefreshToken createRefreshToken(User user) {
		return refreshTokenRepository.findByUser(user).map(existing -> {
			existing.setToken(UUID.randomUUID().toString());
			existing.setExpiryDate(Instant.now().plusMillis(refreshDurationMs));
			return refreshTokenRepository.save(existing);
		}).orElseGet(() -> {
			RefreshToken rt = new RefreshToken();
			rt.setUser(user);
			rt.setToken(UUID.randomUUID().toString());
			rt.setExpiryDate(Instant.now().plusMillis(refreshDurationMs));
			return refreshTokenRepository.save(rt);
		});
	}

	public boolean isExpired(RefreshToken token) {
		return token.getExpiryDate().isBefore(Instant.now());
	}

	public RefreshToken findByToken(String token) {
		return refreshTokenRepository.findByToken(token)
				.orElseThrow(() -> new RefreshTokenException("Refresh token not found"));
	}

	// Handle refresh flow
	public String refreshAccessToken(String requestRefreshToken) {
		RefreshToken token = findByToken(requestRefreshToken);

		if (isExpired(token)) {
			refreshTokenRepository.delete(token);
			throw new RefreshTokenException("Refresh token expired, login again");
		}

		var roles = token.getUser().getRoles().stream().map(r -> r.getName()).toList();
		return jwtUtils.generateAccessToken(token.getUser().getUsername(), token.getUser().getId(), roles);
	}

	// Handle logout flow
	public void logout(String requestRefreshToken) {
		RefreshToken token = findByToken(requestRefreshToken);
		refreshTokenRepository.delete(token);
	}
}
