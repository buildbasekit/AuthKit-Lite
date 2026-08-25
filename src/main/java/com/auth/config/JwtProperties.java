package com.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "JWT secret must be at least 32 characters long for HS256")
        String secret,
        
        @NotNull
        Duration accessTokenTtl,
        
        @NotNull
        Duration refreshTokenTtl,
        
        @NotBlank
        String issuer,
        
        @NotBlank
        String audience
) {
    public JwtProperties {
        if (accessTokenTtl != null && (accessTokenTtl.isNegative() || accessTokenTtl.isZero())) {
            throw new IllegalArgumentException("accessTokenTtl must be positive");
        }
        if (refreshTokenTtl != null && (refreshTokenTtl.isNegative() || refreshTokenTtl.isZero())) {
            throw new IllegalArgumentException("refreshTokenTtl must be positive");
        }
    }
}
