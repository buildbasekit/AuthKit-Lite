package com.auth.exceptions;

public class AccessDeniedBusinessException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AccessDeniedBusinessException(String message) {
		super(message);
	}
}
