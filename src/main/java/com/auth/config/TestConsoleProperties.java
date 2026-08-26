package com.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authkit.test-console")
public record TestConsoleProperties(boolean enabled) {
}
