package com.hotelhost.backend.dto;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String firstName,

		@NotBlank(message = "Los apellidos son obligatorios")
		@Size(max = 150, message = "Los apellidos no pueden superar los 150 caracteres")
		String lastName,

		@NotBlank(message = "El DNI/NIE es obligatorio")
		@Pattern(regexp = "^([0-9]{8}|[XYZxyz][0-9]{7})[A-Za-z]$", message = "El DNI/NIE no tiene un formato válido")
		String dni,

		@NotBlank(message = "El teléfono es obligatorio")
		@Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
		String phone,

		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es válido")
		@Size(max = 255, message = "El email no puede superar los 255 caracteres")
		String email,

		@NotBlank(message = "La contraseña temporal es obligatoria")
		@Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
				message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número")
		String temporaryPassword,

		@NotEmpty(message = "Asigna al menos un edificio")
		Set<Long> buildingIds,

		@NotNull(message = "La fecha de alta es obligatoria")
		LocalDate hireDate,

		@Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
		String notes) {
}
