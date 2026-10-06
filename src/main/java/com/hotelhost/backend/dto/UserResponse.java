package com.hotelhost.backend.dto;

import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;

public record UserResponse(
		Long id,
		String email,
		String firstName,
		String lastName,
		String phone,
		UserRole role,
		boolean mustChangePassword) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getPhone(),
				user.getRole(),
				user.isMustChangePassword());
	}
}
