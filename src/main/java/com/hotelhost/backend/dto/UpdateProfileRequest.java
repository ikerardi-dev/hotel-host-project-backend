package com.hotelhost.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que cada usuario puede cambiar de sí mismo. El email no: es su usuario de acceso y lo gestiona el admin.
 */
public record UpdateProfileRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String firstName,

		@Size(max = 150, message = "Los apellidos no pueden superar los 150 caracteres")
		String lastName,

		@Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
		String phone) {
}
