package com.hotelhost.backend.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Comprueba en cada petición con sesión el estado actual del usuario en la base de datos:
 * - si se ha eliminado o dado de baja, su token deja de valer aunque no haya caducado (401);
 * - mientras tenga la contraseña temporal, solo puede consultar sus datos y cambiarla (403).
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

		if (authentication instanceof JwtAuthenticationToken token) {
			Optional<User> user = userRepository.findById(Long.valueOf(token.getName()));

			if (user.isEmpty() || !user.get().isActive()) {
				ApiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
						"Tu sesión ya no es válida. Vuelve a iniciar sesión");
				return;
			}
			if (user.get().isMustChangePassword() && !isAllowedDuringPasswordChange(request)) {
				ApiErrorWriter.write(response, HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
						"Tienes que cambiar la contraseña temporal antes de continuar");
				return;
			}
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
