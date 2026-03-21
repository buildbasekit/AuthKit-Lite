package com.auth.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EmailAlreadyExistsException(String email) {
		super("Email already in use: " + email);
	}
}
