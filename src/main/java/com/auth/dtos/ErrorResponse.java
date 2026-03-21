package com.auth.dtos;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorResponse {
	private int status;
	private String error;
	private String message;
	private String code;
	private String path;
	private Instant timestamp = Instant.now();

	public ErrorResponse(int status, String error, String message, String code, String path) {
		super();
		this.status = status;
		this.error = error;
		this.message = message;
		this.code = code;
		this.path = path;
		this.timestamp = Instant.now();
	}
}
