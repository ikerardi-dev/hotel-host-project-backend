package com.hotelhost.backend.dto;

import java.time.LocalTime;
import java.util.List;

import com.hotelhost.backend.entity.CleaningStatus;

/**
 * Resumen del día para el admin: cifras y línea de tiempo ordenada por hora.
 */
public record TodayDashboardResponse(
		int checkIns,
		int checkOuts,
		int pendingCleanings,
		int inProgressCleanings,
		int doneCleanings,
		int occupiedApartments,
		int totalApartments,
		List<TimelineEvent> timeline) {

	public enum EventType {
		CHECK_OUT,
		CLEANING,
		CHECK_IN
	}

	/**
	 * Un evento del día. guestName en entradas y salidas; cleaningId, endTime, assignedToName y cleaningStatus
	 * en limpiezas.
	 */
	public record TimelineEvent(
			EventType type,
			LocalTime time,
			Long apartmentId,
			String apartmentCode,
			String buildingName,
			String guestName,
			Long cleaningId,
			LocalTime endTime,
			String assignedToName,
			CleaningStatus cleaningStatus) {
	}
}
