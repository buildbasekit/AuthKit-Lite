package com.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AuthKitApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthKitApplication.class, args);
	}

}
