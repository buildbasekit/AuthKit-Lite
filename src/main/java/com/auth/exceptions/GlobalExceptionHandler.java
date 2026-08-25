package com.auth.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(UsernameAlreadyExistsException.class)
	public ProblemDetail handleUsernameExists(UsernameAlreadyExistsException ex) {
		return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "USERNAME_ALREADY_EXISTS");
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex) {
		return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "EMAIL_ALREADY_EXISTS");
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
		return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS");
	}

	@ExceptionHandler(RefreshTokenException.class)
	public ProblemDetail handleRefreshToken(RefreshTokenException ex) {
		return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_REFRESH_TOKEN");
	}

	@ExceptionHandler(AccessDeniedBusinessException.class)
	public ProblemDetail handleBusinessAccessDenied(AccessDeniedBusinessException ex) {
		return buildProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), "BUSINESS_ACCESS_DENIED");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return buildProblemDetail(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", "ACCESS_DENIED");
	}

	@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
	public ProblemDetail handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
		return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication failed", "UNAUTHORIZED");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		return buildProblemDetail(HttpStatus.CONFLICT, "Duplicate or invalid data provided", "DATA_INTEGRITY_ERROR");
	}

	@ExceptionHandler(RuntimeException.class)
	public org.springframework.http.ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneral(Exception ex) {
		return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "GENERIC_ERROR");
	}

	private ProblemDetail buildProblemDetail(HttpStatus status, String detail, String code) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setProperty("code", code);
		return problem;
	}
}
