package com.auth.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.config.JwtProperties;
import com.auth.dtos.TokenResponse;
import com.auth.entities.RefreshToken;
import com.auth.entities.User;
import com.auth.exceptions.AccessDeniedBusinessException;
import com.auth.exceptions.RefreshTokenException;
import com.auth.repositories.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {
	private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;
	private final TokenService tokenService;

	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties, TokenService tokenService) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtProperties = jwtProperties;
		this.tokenService = tokenService;
	}

	private String hashToken(String rawToken) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Could not hash token", e);
		}
	}

	@Transactional
	public String createRefreshToken(User user) {
		// Execute the delete before inserting so the one-token-per-user constraint
		// cannot observe the old and replacement rows at the same time.
		refreshTokenRepository.deleteByUser(user);

		byte[] randomBytes = new byte[32];
		SECURE_RANDOM.nextBytes(randomBytes);
		String rawToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
		RefreshToken rt = new RefreshToken();
		rt.setUser(user);
		rt.setTokenHash(hashToken(rawToken));
		rt.setExpiryDate(Instant.now().plusMillis(jwtProperties.refreshTokenTtl().toMillis()));
		refreshTokenRepository.save(rt);
		
		return rawToken;
	}

	public boolean isExpired(RefreshToken token) {
		return !token.getExpiryDate().isAfter(Instant.now());
	}

	public RefreshToken findByHashedToken(String rawToken) {
		return refreshTokenRepository.findByTokenHash(hashToken(rawToken))
				.orElseThrow(() -> new RefreshTokenException("Refresh token not found"));
	}

	@Transactional
	public TokenResponse refreshAccessToken(String requestRefreshToken) {
		RefreshToken token = findByHashedToken(requestRefreshToken);

		if (isExpired(token)) {
			refreshTokenRepository.delete(token);
			throw new RefreshTokenException("Refresh token expired, login again");
		}

		User user = token.getUser();
		if (!user.isEnabled()) {
			refreshTokenRepository.delete(token);
			throw new AccessDeniedBusinessException("User account is disabled");
		}

		// Rotation: Delete old token atomically
		int deletedCount = refreshTokenRepository.deleteByTokenHash(token.getTokenHash());
		if (deletedCount == 0) {
			// Token was already deleted by another concurrent request
			throw new RefreshTokenException("Refresh token was already used or revoked");
		}
		
		String newRawRefreshToken = createRefreshToken(user);

		var roles = user.getRoles().stream().map(r -> r.getName()).toList();
		String newAccessToken = tokenService.generateAccessToken(user.getUsername(), user.getId(), roles);
		
		return new TokenResponse(
				newAccessToken,
				newRawRefreshToken,
				"Bearer",
				jwtProperties.accessTokenTtl().toSeconds(),
				jwtProperties.refreshTokenTtl().toSeconds()
		);
	}

	@Transactional
	public void logout(String requestRefreshToken) {
		try {
			RefreshToken token = findByHashedToken(requestRefreshToken);
			refreshTokenRepository.deleteByTokenHash(token.getTokenHash());
		} catch (RefreshTokenException ignored) {
			// If it's already invalid or deleted, we don't care on logout.
		}
	}
}
