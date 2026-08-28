package com.auth.controllers;

import com.auth.dtos.LoginRequest;
import com.auth.dtos.RefreshTokenRequest;
import com.auth.dtos.RegisterRequest;
import com.auth.entities.Role;
import com.auth.entities.User;
import com.auth.repositories.RefreshTokenRepository;
import com.auth.repositories.RoleRepository;
import com.auth.repositories.UserRepository;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

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
	private JsonMapper objectMapper;

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
	}

	@Test
	void testRegisterSuccess() throws Exception {
		RegisterRequest request = new RegisterRequest("newuser", "newuser@example.com", "password123123");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("User registered successfully"));
	}

	@Test
	void testRegisterDuplicateUsername() throws Exception {
		RegisterRequest request = new RegisterRequest("testuser", "other@example.com", "password123123");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
	}

	@Test
	void testRegisterDuplicateEmail() throws Exception {
		RegisterRequest request = new RegisterRequest("otheruser", "testuser@example.com", "password123123");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void testValidationFailure() throws Exception {
		RegisterRequest request = new RegisterRequest("", "not-an-email", "12");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testMalformedJsonRejected() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not-json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void testLoginSuccess() throws Exception {
		LoginRequest request = new LoginRequest("testuser", "password123123");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty());
	}

	@Test
	void testRepeatedLoginReplacesExistingRefreshToken() throws Exception {
		LoginRequest request = new LoginRequest("testuser", "password123123");
		String firstLoginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String secondLoginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andReturn().getResponse().getContentAsString();

		String firstRefreshToken = objectMapper.readTree(firstLoginResponse).get("refreshToken").asString();
		String secondRefreshToken = objectMapper.readTree(secondLoginResponse).get("refreshToken").asString();
		assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RefreshTokenRequest(firstRefreshToken))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testLoginNormalizesUsername() throws Exception {
		LoginRequest request = new LoginRequest("  TESTUSER  ", "password123123");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void testLoginInvalidPassword() throws Exception {
		LoginRequest request = new LoginRequest("testuser", "wrongpassword");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void testLoginDisabledUser() throws Exception {
		User user = userRepository.findByUsername("testuser").get();
		user.setEnabled(false);
		userRepository.save(user);

		LoginRequest request = new LoginRequest("testuser", "password123123");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testRefreshTokenSuccess() throws Exception {
		// Log in to get a refresh token
		LoginRequest loginRequest = new LoginRequest("testuser", "password123123");
		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asString();

		// Refresh token
		RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
		String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(refreshRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andReturn().getResponse().getContentAsString();

		String newRefreshToken = objectMapper.readTree(refreshResponse).get("refreshToken").asString();

		// Ensure old token is revoked
		RefreshTokenRequest oldRefreshRequest = new RefreshTokenRequest(refreshToken);
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(oldRefreshRequest)))
				.andExpect(status().isUnauthorized());
				
		// Ensure new token works
		RefreshTokenRequest newRefreshRequest = new RefreshTokenRequest(newRefreshToken);
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(newRefreshRequest)))
				.andExpect(status().isOk());
	}

	@Test
	void testRefreshDisabledUserRejected() throws Exception {
		LoginRequest loginRequest = new LoginRequest("testuser", "password123123");
		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asString();

		// Disable user
		User user = userRepository.findByUsername("testuser").get();
		user.setEnabled(false);
		userRepository.save(user);

		RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(refreshRequest)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("BUSINESS_ACCESS_DENIED"));
	}

	@Test
	void testLogoutSucceeds() throws Exception {
		LoginRequest loginRequest = new LoginRequest("testuser", "password123123");
		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		var loginJson = objectMapper.readTree(loginResponse);
		String accessToken = loginJson.get("accessToken").asString();
		String refreshToken = loginJson.get("refreshToken").asString();

		// Logout
		RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
		mockMvc.perform(post("/api/auth/logout")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(refreshRequest)))
				.andExpect(status().isOk());

		// Verify refresh fails
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(refreshRequest)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testLogoutRequiresAccessToken() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RefreshTokenRequest("unknown"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testInvalidRefreshTokenRejected() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RefreshTokenRequest("unknown"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void testRegistrationHashesPassword() throws Exception {
		RegisterRequest request = new RegisterRequest("hasheduser", "hashed@example.com", "password123123");
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		User saved = userRepository.findByUsername("hasheduser").orElseThrow();
		assertThat(saved.getPassword()).isNotEqualTo(request.password());
		assertThat(passwordEncoder.matches(request.password(), saved.getPassword())).isTrue();
	}
	
	@Test
	void testConcurrentRefreshIsProtected() throws Exception {
		// Log in to get a refresh token
		LoginRequest loginRequest = new LoginRequest("testuser", "password123123");
		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asString();
		RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
		String requestContent = objectMapper.writeValueAsString(refreshRequest);

		int threads = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch latch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threads);
		
		ConcurrentLinkedQueue<Integer> statuses = new ConcurrentLinkedQueue<>();
		ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

		try {
			for (int i = 0; i < threads; i++) {
				executor.submit(() -> {
					try {
						latch.await();
						statuses.add(mockMvc.perform(post("/api/auth/refresh")
								.contentType(MediaType.APPLICATION_JSON)
								.content(requestContent))
								.andReturn().getResponse().getStatus());
					} catch (Throwable failure) {
						failures.add(failure);
					} finally {
						doneLatch.countDown();
					}
				});
			}

			latch.countDown();
			assertThat(doneLatch.await(15, TimeUnit.SECONDS)).isTrue();
		} finally {
			executor.shutdownNow();
		}

		assertThat(failures).isEmpty();
		assertThat(statuses).hasSize(threads);
		assertThat(statuses.stream().filter(code -> code == 200)).hasSize(1);
		assertThat(statuses.stream().filter(code -> code == 401)).hasSize(threads - 1);
	}
}
