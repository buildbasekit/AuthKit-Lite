package com.auth.services;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.auth.dtos.UserProfileDto;
import com.auth.dtos.UserSummaryDto;
import com.auth.entities.User;
import com.auth.exceptions.AccessDeniedBusinessException;
import com.auth.repositories.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public static final String ROLE_ADMIN = "ROLE_ADMIN";

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserProfileDto getProfile(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new AccessDeniedBusinessException("User not found: " + username));

		Set<String> roles = user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());

		return new UserProfileDto(user.getId(), user.getUsername(), user.getEmail(), roles, user.isEnabled());
	}

	public java.util.List<UserSummaryDto> getAllUsers() {
		return userRepository.findAll().stream().map(u -> new UserSummaryDto(u.getId(), u.getUsername(), u.getEmail()))
				.toList();
	}

	/**
	 * Example business rule: Only ADMINs may access this resource. (Shows how to
	 * enforce business logic beyond @PreAuthorize.)
	 */
	public String restrictedResource(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new AccessDeniedBusinessException("User not found: " + username));

		boolean isAdmin = user.getRoles().stream().anyMatch(r -> ROLE_ADMIN.equals(r.getName()));
		if (!isAdmin) {
			throw new AccessDeniedBusinessException("Basic users cannot access this resource");
		}
		return "Welcome privileged user";
	}
}
