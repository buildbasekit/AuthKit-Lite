package com.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.auth.repositories.UserRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:authkit_lite_no_demo_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"authkit.passkey.enabled=false",
		"authkit.demo-data.enabled=false"
})
@AutoConfigureMockMvc
class ProductionDefaultsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Test
	void publicConsoleAndDisabledPasskeysAreIndependent() throws Exception {
		mockMvc.perform(get("/api-test"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/api-test/index.html"));

		mockMvc.perform(get("/api-test/index.html"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/webauthn/csrf"))
				.andExpect(status().isForbidden());

		assertThat(userRepository.findByUsername("admin")).isEmpty();
		assertThat(userRepository.findByUsername("user")).isEmpty();
	}
}
