package com.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.auth.dtos.TokenResponse;
import com.auth.dtos.MessageResponse;
import com.auth.dtos.LoginRequest;
import com.auth.dtos.RegisterRequest;
import com.auth.dtos.RefreshTokenRequest;
import com.auth.security.AuthService;
import com.auth.security.RefreshTokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenService refreshTokenService;

	public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
		this.authService = authService;
		this.refreshTokenService = refreshTokenService;
	}

	@PostMapping("/register")
	public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
		authService.register(req.username(), req.email(), req.password());
		return ResponseEntity.ok(new MessageResponse("User registered successfully"));
	}

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
		TokenResponse response = authService.login(req);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
		TokenResponse response = refreshTokenService.refreshAccessToken(req.refreshToken());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest req) {
		refreshTokenService.logout(req.refreshToken());
		return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
	}
}
