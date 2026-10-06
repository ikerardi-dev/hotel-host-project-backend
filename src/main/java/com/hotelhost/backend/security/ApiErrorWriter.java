package com.hotelhost.backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Escribe un error con el formato de la API desde los filtros de seguridad,
 * que se ejecutan antes de los controllers y no pasan por el GlobalExceptionHandler.
 */
public final class ApiErrorWriter {

	private ApiErrorWriter() {
	}

	// Los mensajes son fijos y sin caracteres especiales de JSON, por eso se escriben directamente
	public static void write(HttpServletResponse response, HttpStatus status, String code, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"error\":\"" + code + "\",\"message\":\"" + message + "\"}");
	}
}
