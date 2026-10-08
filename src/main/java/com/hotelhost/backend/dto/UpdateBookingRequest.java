package com.hotelhost.backend.dto;

import java.time.LocalDateTime;

import com.hotelhost.backend.entity.BookingChannel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Edición de una reserva. El apartamento no cambia (para eso se cancela y se crea otra)
 * y la limpieza se gestiona desde su propio detalle.
 */
public record UpdateBookingRequest(
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
		String notes) {
}
