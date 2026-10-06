package com.hotelhost.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.UserResponse;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public UserResponse getCurrentUser(Long userId) {
		return UserResponse.from(findActiveUser(userId));
	}

	// Usuario del token: si ya no existe o se ha dado de baja, la sesión deja de valer
	private User findActiveUser(Long userId) {
		return userRepository.findById(userId)
				.filter(User::isActive)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Tienes que iniciar sesión"));
	}
}
