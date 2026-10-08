package com.hotelhost.backend.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Reasignación de una limpieza pendiente por el admin. cleanerId null = sin asignar.
 */
public record UpdateCleaningRequest(
		Long cleanerId,

		@NotNull(message = "Indica la hora de inicio de la limpieza")
		LocalTime startTime,

		@NotNull(message = "Indica la hora de fin de la limpieza")
		LocalTime endTime) {

	@AssertTrue(message = "La limpieza debe terminar después de empezar")
	public boolean isValidTimeRange() {
		return startTime == null || endTime == null || endTime.isAfter(startTime);
	}
}
