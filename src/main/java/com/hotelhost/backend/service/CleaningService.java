package com.hotelhost.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.CleaningResponse;
import com.hotelhost.backend.dto.UpdateCleaningRequest;
import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingStatus;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningChecklistItem;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.security.CurrentUser;

/**
 * Trabajo del día a día con las limpiezas. Un empleado solo puede ver y modificar las suyas; el admin, todas.
 */
@Service
public class CleaningService {

	private final CleaningRepository cleaningRepository;
	private final BookingRepository bookingRepository;
	private final EmployeeService employeeService;

	public CleaningService(
			CleaningRepository cleaningRepository,
			BookingRepository bookingRepository,
			EmployeeService employeeService) {
		this.cleaningRepository = cleaningRepository;
		this.bookingRepository = bookingRepository;
		this.employeeService = employeeService;
	}

	/**
	 * Control del admin: limpiezas de un periodo con filtros opcionales.
	 */
	@Transactional(readOnly = true)
	public List<CleaningResponse> search(
			LocalDate from,
			LocalDate to,
			CleaningStatus status,
			Long buildingId,
			Long employeeId,
			boolean unassigned) {
		DateRanges.validate(from, to);
		return cleaningRepository.search(from, to, status, buildingId, employeeId, unassigned).stream()
				.map(this::toResponse)
				.toList();
	}

	// Calendario del apartamento: sus limpiezas del periodo, sin las canceladas
	@Transactional(readOnly = true)
	public List<CleaningResponse> findByApartment(Long apartmentId, LocalDate from, LocalDate to) {
		DateRanges.validate(from, to);
		return cleaningRepository
				.findByApartmentIdAndCleaningDateBetweenAndStatusNotOrderByCleaningDateAscStartTimeAsc(
						apartmentId, from, to, CleaningStatus.CANCELLED)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * El admin reasigna el empleado (o la deja sin asignar) y cambia la franja de una limpieza pendiente.
	 */
	@Transactional
	public CleaningResponse update(Long id, UpdateCleaningRequest request) {
		Cleaning cleaning = cleaningRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLEANING_NOT_FOUND", "La limpieza no existe"));
		if (cleaning.getStatus() != CleaningStatus.PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, "CLEANING_NOT_EDITABLE",
					"Solo se pueden reasignar las limpiezas pendientes");
		}

		cleaning.setAssignedTo(request.cleanerId() == null ? null : employeeService.findActiveCleaner(request.cleanerId()));
		cleaning.setStartTime(request.startTime());
		cleaning.setEndTime(request.endTime());
		return toResponse(cleaning);
	}

	@Transactional(readOnly = true)
	public List<CleaningResponse> findMine(CurrentUser user) {
		return cleaningRepository.findCurrentByAssignedTo(user.id(), LocalDate.now()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CleaningResponse findById(Long id, CurrentUser user) {
		return toResponse(findAccessible(id, user));
	}

	/**
	 * Solo se permite avanzar: Pendiente → En curso → Hecha. Se guardan las horas reales de inicio y fin.
	 */
	@Transactional
	public CleaningResponse updateStatus(Long id, CleaningStatus newStatus, CurrentUser user) {
		Cleaning cleaning = findAccessible(id, user);
		CleaningStatus current = cleaning.getStatus();

		if (current == CleaningStatus.PENDING && newStatus == CleaningStatus.IN_PROGRESS) {
			cleaning.setStartedAt(LocalDateTime.now());
		} else if (current == CleaningStatus.IN_PROGRESS && newStatus == CleaningStatus.DONE) {
			cleaning.setFinishedAt(LocalDateTime.now());
		} else {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
					"La limpieza no puede pasar de " + current + " a " + newStatus);
		}
		cleaning.setStatus(newStatus);
		return toResponse(cleaning);
	}

	@Transactional
	public CleaningResponse updateChecklistItem(Long id, Long itemId, boolean done, CurrentUser user) {
		Cleaning cleaning = findAccessible(id, user);
		if (cleaning.getStatus() != CleaningStatus.IN_PROGRESS) {
			throw new ApiException(HttpStatus.CONFLICT, "CLEANING_NOT_IN_PROGRESS",
					"Empieza la limpieza para marcar las tareas");
		}

		CleaningChecklistItem item = cleaning.getChecklistItems().stream()
				.filter(candidate -> candidate.getId().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CHECKLIST_ITEM_NOT_FOUND", "La tarea no existe"));
		item.setDone(done);
		return toResponse(cleaning);
	}

	@Transactional
	public CleaningResponse updateIncident(Long id, String incidentNotes, CurrentUser user) {
		Cleaning cleaning = findAccessible(id, user);
		if (cleaning.getStatus() == CleaningStatus.CANCELLED) {
			throw new ApiException(HttpStatus.CONFLICT, "CLEANING_CANCELLED", "La limpieza está cancelada");
		}
		cleaning.setIncidentNotes(incidentNotes == null || incidentNotes.isBlank() ? null : incidentNotes.trim());
		return toResponse(cleaning);
	}

	private Cleaning findAccessible(Long id, CurrentUser user) {
		Cleaning cleaning = cleaningRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLEANING_NOT_FOUND", "La limpieza no existe"));

		boolean assignedToUser = cleaning.getAssignedTo() != null && cleaning.getAssignedTo().getId().equals(user.id());
		if (!user.admin() && !assignedToUser) {
			throw new ApiException(HttpStatus.FORBIDDEN, "CLEANING_NOT_ASSIGNED", "Esta limpieza no está asignada a ti");
		}
		return cleaning;
	}

	private CleaningResponse toResponse(Cleaning cleaning) {
		LocalDateTime nextCheckIn = bookingRepository
				.findFirstByApartmentIdAndStatusAndCheckInGreaterThanEqualOrderByCheckInAsc(
						cleaning.getApartment().getId(), BookingStatus.ACTIVE, cleaning.getCleaningDate().atStartOfDay())
				.map(Booking::getCheckIn)
				.orElse(null);
		return CleaningResponse.from(cleaning, nextCheckIn);
	}
}
