package com.auth.dtos;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
	private Long id;
	private String username;
	private String email;
	private Set<String> roles;
	private boolean enabled;
}
