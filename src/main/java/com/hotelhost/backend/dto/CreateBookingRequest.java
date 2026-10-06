package com.hotelhost.backend.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.hotelhost.backend.entity.BookingChannel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reserva anotada a mano. Si cleaning no es null, se programa la limpieza del día de salida.
 */
public record CreateBookingRequest(
		@NotNull(message = "Elige un apartamento")
		Long apartmentId,

		@NotNull(message = "Elige el canal de la reserva")
		BookingChannel channel,

		@NotBlank(message = "El nombre del huésped es obligatorio")
		@Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
		String guestName,

		@Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
		String guestPhone,

		@Email(message = "El email no es válido")
		@Size(max = 255, message = "El email no puede superar los 255 caracteres")
		String guestEmail,

		@Size(max = 100, message = "La referencia no puede superar los 100 caracteres")
		String reference,

		@NotNull(message = "La fecha de entrada es obligatoria")
		LocalDateTime checkIn,

		@NotNull(message = "La fecha de salida es obligatoria")
		LocalDateTime checkOut,

		@NotNull(message = "Indica el número de huéspedes")
		@Min(value = 1, message = "Tiene que haber al menos un huésped")
		Integer guests,

		@Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
		String notes,

		@Valid
		CleaningPlan cleaning) {

	/**
	 * Limpieza tras la salida. cleanerId es opcional (puede quedar sin asignar).
	 */
	public record CleaningPlan(
			@NotNull(message = "Indica la hora de inicio de la limpieza")
			LocalTime startTime,

			@NotNull(message = "Indica la hora de fin de la limpieza")
			LocalTime endTime,

			Long cleanerId) {

		@AssertTrue(message = "La limpieza debe terminar después de empezar")
		public boolean isValidTimeRange() {
			return startTime == null || endTime == null || endTime.isAfter(startTime);
		}
	}
}
