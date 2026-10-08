package com.hotelhost.backend.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.hotelhost.backend.entity.UserRole;

/**
 * Usuario que hace la petición, sacado del token: su id (claim sub) y si es administrador (claim role).
 */
public record CurrentUser(Long id, boolean admin) {

	public static CurrentUser from(Jwt jwt) {
		return new CurrentUser(Long.valueOf(jwt.getSubject()), UserRole.ADMIN.name().equals(jwt.getClaimAsString("role")));
	}
}
