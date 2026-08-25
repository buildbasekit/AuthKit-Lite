package com.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "authkit.passkey")
public record PasskeyProperties(
        boolean enabled,
        @NotBlank String rpName,
        @NotBlank String rpId,
        @NotEmpty List<String> allowedOrigins
) {
}
