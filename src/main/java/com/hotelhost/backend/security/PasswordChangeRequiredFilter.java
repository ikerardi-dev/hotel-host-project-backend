package com.hotelhost.backend.security;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hotelhost.backend.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Mientras el usuario tenga la contraseña temporal, solo puede consultar sus datos y cambiarla.
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;

	public PasswordChangeRequiredFilter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication instanceof JwtAuthenticationToken token
				&& !isAllowedDuringPasswordChange(request)
				&& userRepository.existsByIdAndMustChangePasswordTrue(Long.valueOf(token.getName()))) {
			ApiErrorWriter.write(response, HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
					"Tienes que cambiar la contraseña temporal antes de continuar");
			return;
		}

		chain.doFilter(request, response);
	}

	private boolean isAllowedDuringPasswordChange(HttpServletRequest request) {
		String path = request.getRequestURI();
		String method = request.getMethod();
		return (HttpMethod.GET.matches(method) && path.equals("/api/me"))
				|| (HttpMethod.PUT.matches(method) && path.equals("/api/me/password"));
	}
}
