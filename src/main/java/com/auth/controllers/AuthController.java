package com.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.auth.dtos.JwtResponse;
import com.auth.dtos.LoginRequest;
import com.auth.dtos.RegisterRequest;
import com.auth.security.AuthService;
import com.auth.security.RefreshTokenService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenService refreshTokenService;

	public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
		super();
		this.authService = authService;
		this.refreshTokenService = refreshTokenService;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
		var user = authService.register(req.getUsername(), req.getEmail(), req.getPassword());
		return ResponseEntity.ok(Map.of("message", "User registered", "username", user.getUsername()));

	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest req) {
		JwtResponse response = authService.login(req);
		return ResponseEntity.ok(new JwtResponse(response.getAccessToken(), response.getRefreshToken(),
				response.getTokenType(), response.getExpiresInMillis()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
		String requestRefreshToken = body.get("refreshToken");
		String newAccessToken = refreshTokenService.refreshAccessToken(requestRefreshToken);
		return ResponseEntity.ok(Map.of("accessToken", newAccessToken, "refreshToken", requestRefreshToken));
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
		try {
			String requestRefreshToken = body.get("refreshToken");
			refreshTokenService.logout(requestRefreshToken);
			return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
		}catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

}
