package com.auth.dtos;

public record UserSummaryDto(
	Long id,
	String username,
	String email
) {}
