package com.hotelhost.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.LoginRequest;
import com.hotelhost.backend.dto.LoginResponse;
import com.hotelhost.backend.dto.UserResponse;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.UserRepository;
import com.hotelhost.backend.security.JwtService;
import com.hotelhost.backend.security.JwtService.IssuedToken;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		// Mismo error si no existe el email o la contraseña no coincide: no se revela cuál de los dos falla
		User user = userRepository.findByEmailIgnoreCase(request.email().trim())
				.filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email o contraseña incorrectos"));

		if (!user.isActive()) {
			throw new ApiException(
					HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Tu cuenta está dada de baja. Contacta con el administrador");
		}

		IssuedToken token = jwtService.generateToken(user);
		return new LoginResponse(token.value(), token.expiresAt(), UserResponse.from(user));
	}
}
