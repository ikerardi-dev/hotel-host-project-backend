package com.hotelhost.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.hotelhost.backend.entity.BedType;

/**
 * Apartamento con su estado actual y el huésped actual o el próximo (null si no hay).
 */
public record ApartmentResponse(
		Long id,
		String code,
		int capacity,
		BuildingSummary building,
		List<BedSummary> beds,
		ApartmentStatus status,
		GuestSummary currentGuest,
		GuestSummary nextGuest) {

	public record BuildingSummary(Long id, String name) {
	}

	public record BedSummary(BedType type, int quantity) {
	}

	public record GuestSummary(Long bookingId, String guestName, LocalDateTime checkIn, LocalDateTime checkOut) {
	}

	public enum ApartmentStatus {
		OCCUPIED,
		PENDING_CLEANING,
		READY,
		FREE
	}
}
