package com.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.auth.config.DemoDataInitializer;
import com.auth.repositories.UserRepository;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:authkit_lite_default_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"authkit.demo-data.enabled=true"
})
class H2DefaultConfigurationTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DemoDataInitializer demoDataInitializer;

	@Test
	void defaultDatabaseStartsAndSeedsDemoUsers() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
		}
		assertThat(userRepository.findByUsername("admin")).isPresent();
		assertThat(userRepository.findByUsername("user")).isPresent();

		userRepository.delete(userRepository.findByUsername("admin").orElseThrow());
		demoDataInitializer.run();

		assertThat(userRepository.findByUsername("admin")).isEmpty();
		assertThat(userRepository.findByUsername("user")).isPresent();
	}
}
