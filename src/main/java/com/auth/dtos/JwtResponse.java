package com.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private Long expiresInMillis;
}
