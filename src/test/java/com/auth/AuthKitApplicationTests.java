package com.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestAuthKitApplication.class)
class AuthKitApplicationTests {

	@Test
	void contextLoads() {
	}

}
