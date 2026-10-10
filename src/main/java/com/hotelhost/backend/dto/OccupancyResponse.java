package com.hotelhost.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.hotelhost.backend.entity.BookingChannel;
import com.hotelhost.backend.entity.CleaningStatus;

/**
 * Planning de ocupación de un edificio en un periodo: sus apartamentos, sus reservas y sus limpiezas.
 */
public record OccupancyResponse(
		List<ApartmentRow> apartments,
		List<BookingBar> bookings,
		List<CleaningMark> cleanings) {

	public record ApartmentRow(Long id, String code, int capacity) {
	}

	public record BookingBar(
			Long id,
			Long apartmentId,
			String guestName,
			BookingChannel channel,
			LocalDateTime checkIn,
			LocalDateTime checkOut) {
	}

	public record CleaningMark(Long id, Long apartmentId, LocalDate date, CleaningStatus status) {
	}
}
