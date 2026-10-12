package com.hotelhost.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.ChangePasswordRequest;
import com.hotelhost.backend.dto.UpdateProfileRequest;
import com.hotelhost.backend.dto.UserResponse;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public UserResponse getCurrentUser(Long userId) {
		return UserResponse.from(findActiveUser(userId));
	}

	@Transactional
	public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
		User user = findActiveUser(userId);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName() == null ? "" : request.lastName().trim());
		user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse changePassword(Long userId, ChangePasswordRequest request) {
		User user = findActiveUser(userId);

		// Fuera del primer acceso hay que confirmar la contraseña actual
		if (!user.isMustChangePassword()
				&& (request.currentPassword() == null
						|| !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "La contraseña actual no es correcta");
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setMustChangePassword(false);
		return UserResponse.from(user);
	}

	// Usuario del token: si ya no existe o se ha dado de baja, la sesión deja de valer
	private User findActiveUser(Long userId) {
		return userRepository.findById(userId)
				.filter(User::isActive)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Tienes que iniciar sesión"));
	}
}
