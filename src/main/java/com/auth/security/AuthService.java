package com.auth.security;

import com.auth.dtos.JwtResponse;
import com.auth.dtos.LoginRequest;
import com.auth.entities.Role;
import com.auth.entities.User;
import com.auth.exceptions.EmailAlreadyExistsException;
import com.auth.exceptions.InvalidCredentialsException;
import com.auth.exceptions.UsernameAlreadyExistsException;
import com.auth.repositories.RoleRepository;
import com.auth.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtils jwtUtils;
	private final RefreshTokenService refreshTokenService;
	
	@Value("${app.jwt.refresh-token-expiration-ms}")
	private Long refreshDurationMs;

	public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
			JwtUtils jwtUtils, RefreshTokenService refreshTokenService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtils = jwtUtils;
		this.refreshTokenService = refreshTokenService;
	}

	public User register(String username, String email, String password) {
		if (userRepository.existsByUsername(username)) {
			throw new UsernameAlreadyExistsException(username);
		}
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException(email);
		}
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(password));
		Role userRole = roleRepository.findByName("ROLE_USER")
				.orElseThrow(() -> new RuntimeException("ROLE_USER not set in DB"));
		user.setRoles(Set.of(userRole));
		return userRepository.save(user);
	}

	public JwtResponse login(LoginRequest req) {
		var userOpt = userRepository.findByUsername(req.getUsername());
		if (userOpt.isEmpty())
			throw new InvalidCredentialsException();
		var user = userOpt.get();
		if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
			throw new InvalidCredentialsException();
		}

		var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
		String accessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getId(), roles);
		var refreshTokenEntity = refreshTokenService.createRefreshToken(user);
		return new JwtResponse(accessToken, refreshTokenEntity.getToken(), "Bearer", refreshDurationMs);
	}
}
