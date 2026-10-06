package com.hotelhost.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que lanzan los services. El GlobalExceptionHandler lo convierte en un ApiError.
 * Ejemplo: throw new ApiException(HttpStatus.CONFLICT, "BOOKING_OVERLAP", "Las fechas se solapan con otra reserva");
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
