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

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserProfileDto getProfile(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new AccessDeniedBusinessException("User not found: " + username));

		Set<String> roles = user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());

		return new UserProfileDto(user.getId(), user.getUsername(), user.getEmail(), roles, user.isEnabled());
	}

	public org.springframework.data.domain.Page<UserSummaryDto> getAllUsers(org.springframework.data.domain.Pageable pageable) {
		return userRepository.findAll(pageable)
				.map(u -> new UserSummaryDto(u.getId(), u.getUsername(), u.getEmail()));
	}
}
