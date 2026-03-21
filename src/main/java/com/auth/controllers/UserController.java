package com.auth.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.auth.dtos.UserProfileDto;
import com.auth.dtos.UserSummaryDto;
import com.auth.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	// Existing simple self-check
	@GetMapping("/me")
	public ResponseEntity<?> me(Authentication authentication) {
		return ResponseEntity
				.ok(Map.of("username", authentication.getName(), "roles", authentication.getAuthorities()));
	}

	// New: profile via service (any authenticated user)
	@GetMapping("/profile")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserProfileDto> profile(Authentication authentication) {
		return ResponseEntity.ok(userService.getProfile(authentication.getName()));
	}

	// New: admin-only list users
	@GetMapping("/all")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	// New: business-rule restricted (role may pass, rule may still block)
	@GetMapping("/restricted")
	@PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
	public ResponseEntity<?> restricted(Authentication authentication) {
		String msg = userService.restrictedResource(authentication.getName());
		return ResponseEntity.ok(Map.of("message", msg));
	}

	// Keep your existing admin-only test if you like
	@GetMapping("/admin-only")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<?> adminOnly() {
		return ResponseEntity.ok(Map.of("message", "Hello admin"));
	}
}
