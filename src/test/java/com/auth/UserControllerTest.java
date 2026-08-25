package com.auth.controllers;

import com.auth.dtos.LoginRequest;
import com.auth.entities.Role;
import com.auth.entities.User;
import com.auth.repositories.RefreshTokenRepository;
import com.auth.repositories.RoleRepository;
import com.auth.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(com.auth.TestAuthKitApplication.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		if (roleRepository.findByName("ROLE_USER").isEmpty()) {
			Role roleUser = new Role();
			roleUser.setName("ROLE_USER");
			roleRepository.save(roleUser);
		}
		if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
			Role roleAdmin = new Role();
			roleAdmin.setName("ROLE_ADMIN");
			roleRepository.save(roleAdmin);
		}

		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();

		User testUser = new User();
		testUser.setUsername("testuser");
		testUser.setEmail("testuser@example.com");
		testUser.setPassword(passwordEncoder.encode("password123123"));
		testUser.setRoles(Set.of(roleRepository.findByName("ROLE_USER").get()));
		testUser.setEnabled(true);
		userRepository.save(testUser);

		User adminUser = new User();
		adminUser.setUsername("adminuser");
		adminUser.setEmail("adminuser@example.com");
		adminUser.setPassword(passwordEncoder.encode("password123123"));
		adminUser.setRoles(Set.of(roleRepository.findByName("ROLE_ADMIN").get()));
		adminUser.setEnabled(true);
		userRepository.save(adminUser);
	}

	private String getTokenFor(String username, String password) throws Exception {
		LoginRequest loginRequest = new LoginRequest(username, password);
		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(loginResponse).get("accessToken").asText();
	}

	@Test
	void testUnauthenticatedAccessRejected() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testAuthenticatedUserCanAccessProfile() throws Exception {
		String token = getTokenFor("testuser", "password123123");

		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("testuser"))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
	}

	@Test
	void testNormalUserCannotAccessAdminEndpoint() throws Exception {
		String token = getTokenFor("testuser", "password123123");

		mockMvc.perform(get("/api/users")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void testAdminCanAccessAdminEndpoint() throws Exception {
		String token = getTokenFor("adminuser", "password123123");

		mockMvc.perform(get("/api/users")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}
}
