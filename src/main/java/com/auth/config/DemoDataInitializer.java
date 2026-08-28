package com.auth.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.auth.entities.Role;
import com.auth.entities.User;
import com.auth.repositories.RoleRepository;
import com.auth.repositories.UserRepository;

@Component
@ConditionalOnProperty(prefix = "authkit.demo-data", name = "enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DemoDataInitializer(RoleRepository roleRepository, UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}

		Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
		User admin = new User();
		admin.setUsername("admin");
		admin.setEmail("admin@example.com");
		admin.setPassword(passwordEncoder.encode("password123123"));
		admin.setRoles(Set.of(adminRole));
		userRepository.save(admin);

		Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
		User user = new User();
		user.setUsername("user");
		user.setEmail("user@example.com");
		user.setPassword(passwordEncoder.encode("password123123"));
		user.setRoles(Set.of(userRole));
		userRepository.save(user);
	}
}
