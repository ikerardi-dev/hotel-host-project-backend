package com.hotelhost.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * currentPassword no se pide en el primer acceso (el usuario acaba de entrar con la contraseña temporal).
 */
public record ChangePasswordRequest(
		String currentPassword,

		@NotBlank(message = "La nueva contraseña es obligatoria")
		@Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
				message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número")
		String newPassword) {
}
