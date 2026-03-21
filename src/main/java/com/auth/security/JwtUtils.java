package com.auth.security;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

@Component
public class JwtUtils {
	@Value("${app.jwt.secret}")
	private String jwtSecret;

	@Value("${app.jwt.access-token-expiration-ms}")
	private Long accessTokenMs;

	public String generateAccessToken(String username, Long userId, List<String> roles) {
		Algorithm algorithm = Algorithm.HMAC256(jwtSecret.getBytes());
		long now = System.currentTimeMillis();
		return JWT.create().withSubject(username).withClaim("uid", userId)
				.withArrayClaim("roles", roles.toArray(new String[0])).withIssuedAt(new Date(now))
				.withExpiresAt(new Date(now + accessTokenMs)).sign(algorithm);
	}

	public DecodedJWT validateAndDecode(String token) {
		Algorithm algorithm = Algorithm.HMAC256(jwtSecret.getBytes());
		return JWT.require(algorithm).build().verify(token);
	}
}
