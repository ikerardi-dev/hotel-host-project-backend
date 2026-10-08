package com.hotelhost.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.ChangePasswordRequest;
import com.hotelhost.backend.dto.CleaningResponse;
import com.hotelhost.backend.dto.UserResponse;
import com.hotelhost.backend.security.CurrentUser;
import com.hotelhost.backend.service.CleaningService;
import com.hotelhost.backend.service.UserService;

import jakarta.validation.Valid;

/**
 * Datos del usuario que ha iniciado sesión (el del token).
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

	private final UserService userService;
	private final CleaningService cleaningService;

	public MeController(UserService userService, CleaningService cleaningService) {
		this.userService = userService;
		this.cleaningService = cleaningService;
	}

	@GetMapping
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return userService.getCurrentUser(userId(jwt));
	}

	@PutMapping("/password")
	public UserResponse changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
		return userService.changePassword(userId(jwt), request);
	}

	// Limpiezas asignadas al usuario: desde hoy en adelante y las atrasadas sin terminar
	@GetMapping("/cleanings")
	public List<CleaningResponse> myCleanings(@AuthenticationPrincipal Jwt jwt) {
		return cleaningService.findMine(CurrentUser.from(jwt));
	}

	private Long userId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}
}
