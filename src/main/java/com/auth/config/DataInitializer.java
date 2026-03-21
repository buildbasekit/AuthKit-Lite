package com.auth.config;

import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.auth.entities.Role;
import com.auth.entities.User;
import com.auth.repositories.RoleRepository;
import com.auth.repositories.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public static class Roles {
		public static final String USER = "ROLE_USER";
		public static final String ADMIN = "ROLE_ADMIN";
	}

	@Override
	@Transactional
	public void run(String... args) {
		// Ensure roles exist
		List<String> defaultRoles = List.of(Roles.USER, Roles.ADMIN);
		for (String roleName : defaultRoles) {
			roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(null, roleName)));
		}

		// Create dummy admin user if not exists
		if (userRepository.findByUsername("admin").isEmpty()) {
			Role adminRole = roleRepository.findByName(Roles.ADMIN).get();
			User admin = new User();
			admin.setUsername("admin");
			admin.setEmail("admin@example.com");
			admin.setPassword(passwordEncoder.encode("admin123"));
			admin.setRoles(Set.of(adminRole));
			userRepository.save(admin);
		}

		// Create dummy normal user if not exists
		if (userRepository.findByUsername("user").isEmpty()) {
			Role userRole = roleRepository.findByName(Roles.USER).get();
			User user = new User();
			user.setUsername("user");
			user.setEmail("user@example.com");
			user.setPassword(passwordEncoder.encode("user123"));
			user.setRoles(Set.of(userRole));
			userRepository.save(user);
		}
	}
}
