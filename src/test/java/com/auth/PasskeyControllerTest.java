package com.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestAuthKitApplication.class)
class PasskeyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	private CsrfSession csrfSession() throws Exception {
		MvcResult result = mockMvc.perform(get("/webauthn/csrf"))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andReturn();
		String token = jsonMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
		return new CsrfSession(result.getResponse().getCookie("XSRF-TOKEN"), token);
	}

	private record CsrfSession(Cookie cookie, String token) { }

	@Test
	void unauthenticatedPasskeyListIsRejected() throws Exception {
		mockMvc.perform(get("/api/users/me/passkeys"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanListPasskeys() throws Exception {
		mockMvc.perform(get("/api/users/me/passkeys")
				.with(jwt().jwt(token -> token.subject("testuser"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void csrfEndpointExposesCookieAndToken() throws Exception {
		mockMvc.perform(get("/webauthn/csrf"))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void authenticationOptionsArePublicButRequireCsrf() throws Exception {
		mockMvc.perform(post("/webauthn/authenticate/options"))
				.andExpect(status().isForbidden());

		CsrfSession csrf = csrfSession();
		mockMvc.perform(post("/webauthn/authenticate/options")
				.cookie(csrf.cookie())
				.header("X-XSRF-TOKEN", csrf.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.challenge").isNotEmpty())
				.andExpect(jsonPath("$.rpId").value("localhost"));
	}

	@Test
	void registrationOptionsRequireJwtAndCsrf() throws Exception {
		CsrfSession csrf = csrfSession();
		mockMvc.perform(post("/webauthn/register/options")
				.cookie(csrf.cookie())
				.header("X-XSRF-TOKEN", csrf.token()))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/webauthn/register/options")
				.with(jwt().jwt(token -> token.subject("testuser")))
				.cookie(csrf.cookie())
				.header("X-XSRF-TOKEN", csrf.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.name").value("testuser"))
				.andExpect(jsonPath("$.challenge").isNotEmpty());
	}

	@Test
	void malformedRegistrationAndAssertionAreRejected() throws Exception {
		CsrfSession csrf = csrfSession();
		mockMvc.perform(post("/webauthn/register")
				.with(jwt().jwt(token -> token.subject("testuser")))
				.cookie(csrf.cookie())
				.header("X-XSRF-TOKEN", csrf.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/login/webauthn")
				.cookie(csrf.cookie())
				.header("X-XSRF-TOKEN", csrf.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void nativeCredentialDeletionEnforcesOwnership() throws Exception {
		CsrfSession csrf = csrfSession();
		mockMvc.perform(delete("/webauthn/register/dGVzdA")
				.with(jwt().jwt(token -> token.subject("testuser")))
				.cookie(csrf.cookie())
				.header("X-XSRF-TOKEN", csrf.token()))
				.andExpect(status().isForbidden());
	}

	@Test
	void corsAllowsConfiguredOriginAndRejectsUntrustedOrigin() throws Exception {
		mockMvc.perform(options("/api/users/me")
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));

		mockMvc.perform(options("/api/users/me")
				.header(HttpHeaders.ORIGIN, "https://untrusted.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
	}
}
