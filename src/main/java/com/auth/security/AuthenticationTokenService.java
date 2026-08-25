package com.auth.security;

import com.auth.config.JwtProperties;
import com.auth.dtos.TokenResponse;
import com.auth.entities.Role;
import com.auth.entities.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthenticationTokenService {

    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public AuthenticationTokenService(TokenService tokenService, RefreshTokenService refreshTokenService, JwtProperties jwtProperties) {
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    public TokenResponse createTokenResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String accessToken = tokenService.generateAccessToken(user.getUsername(), user.getId(), roles);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtProperties.accessTokenTtl().toSeconds(),
                jwtProperties.refreshTokenTtl().toSeconds()
        );
    }
}
