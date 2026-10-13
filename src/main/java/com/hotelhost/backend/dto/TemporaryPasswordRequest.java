package com.hotelhost.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Nueva contraseña temporal que el admin da a un empleado que ha olvidado la suya.
 */
public record TemporaryPasswordRequest(
		@NotBlank(message = "La contraseña temporal es obligatoria")
		@Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
				message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número")
		String temporaryPassword) {
}
