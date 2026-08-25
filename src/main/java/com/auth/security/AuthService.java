package com.auth.security;


import com.auth.dtos.TokenResponse;
import com.auth.dtos.LoginRequest;
import com.auth.entities.Role;
import com.auth.entities.User;
import com.auth.exceptions.EmailAlreadyExistsException;
import com.auth.exceptions.InvalidCredentialsException;
import com.auth.exceptions.UsernameAlreadyExistsException;
import com.auth.repositories.RoleRepository;
import com.auth.repositories.UserRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	private final RefreshTokenService refreshTokenService;
	private final AuthenticationManager authenticationManager;
	private final AuthenticationTokenService authenticationTokenService;

	public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokenService,
			AuthenticationManager authenticationManager, AuthenticationTokenService authenticationTokenService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
		this.authenticationManager = authenticationManager;
		this.authenticationTokenService = authenticationTokenService;
	}

	public User register(String username, String email, String password) {
		String normalizedUsername = username.trim().toLowerCase();
		String normalizedEmail = email.trim().toLowerCase();
		
		if (userRepository.existsByUsername(normalizedUsername)) {
			throw new UsernameAlreadyExistsException(normalizedUsername);
		}
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new EmailAlreadyExistsException(normalizedEmail);
		}
		User user = new User();
		user.setUsername(normalizedUsername);
		user.setEmail(normalizedEmail);
		user.setPassword(passwordEncoder.encode(password));
		Role userRole = roleRepository.findByName("ROLE_USER")
				.orElseThrow(() -> new RuntimeException("ROLE_USER not set in DB"));
		user.setRoles(Set.of(userRole));
		return userRepository.save(user);
	}

	public TokenResponse login(LoginRequest req) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(req.username(), req.password())
			);
		} catch (AuthenticationException e) {
			throw new InvalidCredentialsException();
		}

		var user = userRepository.findByUsername(req.username())
				.orElseThrow(InvalidCredentialsException::new);

		return authenticationTokenService.createTokenResponse(user);
	}
}
