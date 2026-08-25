package com.auth.dtos;

import java.util.Set;

public record UserProfileDto(
	Long id,
	String username,
	String email,
	Set<String> roles,
	boolean enabled
) {}
