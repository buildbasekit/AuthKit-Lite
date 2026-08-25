package com.auth.dtos;

public record TokenResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	Long expiresInSeconds,
	Long refreshTokenExpiresInSeconds
) {}
