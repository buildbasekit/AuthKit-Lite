package com.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.auth.dtos.UserProfileDto;
import com.auth.dtos.UserSummaryDto;
import com.auth.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserProfileDto> me(Authentication authentication) {
		return ResponseEntity.ok(userService.getProfile(authentication.getName()));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<org.springframework.data.domain.Page<UserSummaryDto>> getAllUsers(org.springframework.data.domain.Pageable pageable) {
		return ResponseEntity.ok(userService.getAllUsers(pageable));
	}
}
