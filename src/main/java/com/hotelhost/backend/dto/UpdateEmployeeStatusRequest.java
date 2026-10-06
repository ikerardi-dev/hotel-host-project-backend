package com.hotelhost.backend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeStatusRequest(
		@NotNull(message = "Indica si el empleado está activo")
		Boolean active) {
}
