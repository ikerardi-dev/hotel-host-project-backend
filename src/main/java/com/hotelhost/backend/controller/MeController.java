package com.hotelhost.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.UserResponse;
import com.hotelhost.backend.service.UserService;

/**
 * Datos del usuario que ha iniciado sesión (el del token).
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

	private final UserService userService;

	public MeController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return userService.getCurrentUser(Long.valueOf(jwt.getSubject()));
	}
}
