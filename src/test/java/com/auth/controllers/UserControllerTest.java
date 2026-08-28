package com.auth.controllers;

import com.auth.dtos.LoginRequest;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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
	private JsonMapper objectMapper;

	@Autowired
	private JwtEncoder jwtEncoder;

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

		return objectMapper.readTree(loginResponse).get("accessToken").asString();
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

	@Test
	void testMalformedJwtRejected() throws Exception {
		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testExpiredJwtRejected() throws Exception {
		String token = encodeToken("authkit", List.of("authkit-api"), Instant.now().minusSeconds(60));

		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testIncorrectIssuerRejected() throws Exception {
		String token = encodeToken("other-issuer", List.of("authkit-api"), Instant.now().plusSeconds(300));

		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testIncorrectAudienceRejected() throws Exception {
		String token = encodeToken("authkit", List.of("other-api"), Instant.now().plusSeconds(300));

		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	private String encodeToken(String issuer, List<String> audience, Instant expiresAt) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.issuedAt(now.minusSeconds(120))
				.expiresAt(expiresAt)
				.subject("testuser")
				.audience(audience)
				.claim("roles", List.of("ROLE_USER"))
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
}
