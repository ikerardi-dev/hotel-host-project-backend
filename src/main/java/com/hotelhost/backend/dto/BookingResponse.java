package com.hotelhost.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingChannel;
import com.hotelhost.backend.entity.BookingStatus;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.entity.User;

public record BookingResponse(
		Long id,
		Long apartmentId,
		String apartmentCode,
		String buildingName,
		String guestName,
		String guestPhone,
		String guestEmail,
		String reference,
		BookingChannel channel,
		LocalDateTime checkIn,
		LocalDateTime checkOut,
		int guests,
		String notes,
		BookingStatus status,
		CleaningSummary cleaning) {

	public record CleaningSummary(
			Long id,
			LocalDate date,
			LocalTime startTime,
			LocalTime endTime,
			CleaningStatus status,
			Long assignedToId,
			String assignedToName) {

		static CleaningSummary from(Cleaning cleaning) {
			User cleaner = cleaning.getAssignedTo();
			return new CleaningSummary(
					cleaning.getId(),
					cleaning.getCleaningDate(),
					cleaning.getStartTime(),
					cleaning.getEndTime(),
					cleaning.getStatus(),
					cleaner == null ? null : cleaner.getId(),
					cleaning.assigneeDisplayName());
		}
	}

	public static BookingResponse from(Booking booking, Cleaning cleaning) {
		return new BookingResponse(
				booking.getId(),
				booking.getApartment().getId(),
				booking.getApartment().getCode(),
				booking.getApartment().getBuilding().getName(),
				booking.getGuestName(),
				booking.getGuestPhone(),
				booking.getGuestEmail(),
				booking.getReference(),
				booking.getChannel(),
				booking.getCheckIn(),
				booking.getCheckOut(),
				booking.getGuests(),
				booking.getNotes(),
				booking.getStatus(),
				cleaning == null ? null : CleaningSummary.from(cleaning));
	}
}
