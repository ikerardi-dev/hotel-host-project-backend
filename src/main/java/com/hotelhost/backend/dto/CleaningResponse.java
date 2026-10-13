package com.hotelhost.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import com.hotelhost.backend.dto.ApartmentResponse.BedSummary;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.entity.User;

/**
 * Limpieza con lo que necesita el empleado: apartamento y camas, salida y entrada que la rodean, y checklist.
 */
public record CleaningResponse(
		Long id,
		LocalDate date,
		LocalTime startTime,
		LocalTime endTime,
		CleaningStatus status,
		LocalDateTime startedAt,
		LocalDateTime finishedAt,
		String incidentNotes,
		ApartmentSummary apartment,
		AssigneeSummary assignedTo,
		LocalDateTime previousCheckOut,
		LocalDateTime nextCheckIn,
		List<ChecklistItem> checklist) {

	public record ApartmentSummary(Long id, String code, String buildingName, int capacity, List<BedSummary> beds) {
	}

	// id null: empleado eliminado, del que solo queda el nombre en el historial
	public record AssigneeSummary(Long id, String name) {
	}

	public record ChecklistItem(Long id, String label, boolean done) {
	}

	public static CleaningResponse from(Cleaning cleaning, LocalDateTime nextCheckIn) {
		var apartment = cleaning.getApartment();
		List<BedSummary> beds = apartment.getBeds().stream()
				.sorted(Comparator.comparing(bed -> bed.getType().ordinal()))
				.map(bed -> new BedSummary(bed.getType(), bed.getQuantity()))
				.toList();
		User cleaner = cleaning.getAssignedTo();
		String assigneeName = cleaning.assigneeDisplayName();
		AssigneeSummary assignee = assigneeName == null
				? null
				: new AssigneeSummary(cleaner == null ? null : cleaner.getId(), assigneeName);

		return new CleaningResponse(
				cleaning.getId(),
				cleaning.getCleaningDate(),
				cleaning.getStartTime(),
				cleaning.getEndTime(),
				cleaning.getStatus(),
				cleaning.getStartedAt(),
				cleaning.getFinishedAt(),
				cleaning.getIncidentNotes(),
				new ApartmentSummary(apartment.getId(), apartment.getCode(), apartment.getBuilding().getName(),
						apartment.getCapacity(), beds),
				assignee,
				cleaning.getBooking() == null ? null : cleaning.getBooking().getCheckOut(),
				nextCheckIn,
				cleaning.getChecklistItems().stream()
						.map(item -> new ChecklistItem(item.getId(), item.getLabel(), item.isDone()))
						.toList());
	}
}
