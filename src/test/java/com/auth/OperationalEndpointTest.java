package com.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestAuthKitApplication.class)
class OperationalEndpointTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthAndInfoArePublic() throws Exception {
		mockMvc.perform(get("/actuator"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.health.href").exists())
				.andExpect(jsonPath("$._links.info.href").exists());

		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));

		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isOk());
	}

	@Test
	void fallbackChainDeniesUnknownPaths() throws Exception {
		mockMvc.perform(get("/unknown"))
				.andExpect(status().isForbidden());
	}

	@Test
	void browserApiTestConsoleIsPublicButScopeRemainsNarrow() throws Exception {
		mockMvc.perform(get("/api-test/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("AuthKit-Lite API Test Console")));

		mockMvc.perform(get("/api-test/app.js"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/not-api-test/index.html"))
				.andExpect(status().isForbidden());
	}
}
