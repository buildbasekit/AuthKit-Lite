package com.auth.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.auth.dtos.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// Custom Exceptions

	@ExceptionHandler(UsernameAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleUsernameExists(UsernameAlreadyExistsException ex,
			HttpServletRequest request) {
		return buildError(HttpStatus.CONFLICT, ex.getMessage(), "USERNAME_ALREADY_EXISTS", request);
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
		return buildError(HttpStatus.CONFLICT, ex.getMessage(), "EMAIL_ALREADY_EXISTS", request);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex,
			HttpServletRequest request) {
		return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS", request);
	}

	@ExceptionHandler(RefreshTokenException.class)
	public ResponseEntity<ErrorResponse> handleRefreshToken(RefreshTokenException ex, HttpServletRequest request) {
		return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), "INVALID_REFRESH_TOKEN", request);
	}

	@ExceptionHandler(AccessDeniedBusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessAccessDenied(AccessDeniedBusinessException ex,
			HttpServletRequest request) {
		return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), "BUSINESS_ACCESS_DENIED", request);
	}

	// General Exceptions
	
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
	    ErrorResponse error = new ErrorResponse(
	        HttpStatus.FORBIDDEN.value(),
	        "Forbidden",
	        "You do not have permission to access this resource",
	        "ACCESS_DENIED",
	        request.getRequestURI()
	    );
	    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	@ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			org.springframework.web.bind.MethodArgumentNotValidException ex, HttpServletRequest request) {

		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage()).findFirst().orElse("Validation failed");

		return buildError(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR", request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		return buildError(HttpStatus.CONFLICT, "Duplicate or invalid data: " + ex.getMostSpecificCause().getMessage(),
				"DATA_INTEGRITY_ERROR", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
		return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "GENERIC_ERROR", request);
	}

	// helper method
	private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message, String code,
			HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse(status.value(), status.getReasonPhrase(), message, code,
				request.getRequestURI());
		return ResponseEntity.status(status).body(error);
	}
}
