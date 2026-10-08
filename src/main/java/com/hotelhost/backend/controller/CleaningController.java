package com.hotelhost.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.CleaningResponse;
import com.hotelhost.backend.dto.UpdateCleaningRequest;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.dto.UpdateChecklistItemRequest;
import com.hotelhost.backend.dto.UpdateCleaningStatusRequest;
import com.hotelhost.backend.dto.UpdateIncidentRequest;
import com.hotelhost.backend.security.CurrentUser;
import com.hotelhost.backend.service.CleaningService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cleanings")
public class CleaningController {

	private final CleaningService cleaningService;

	public CleaningController(CleaningService cleaningService) {
		this.cleaningService = cleaningService;
	}

	// Solo admin (ver SecurityConfig). Ejemplo: /api/cleanings?from=2026-10-01&to=2026-10-15&status=PENDING
	@GetMapping
	public List<CleaningResponse> search(
			@RequestParam LocalDate from,
			@RequestParam LocalDate to,
			@RequestParam(required = false) CleaningStatus status,
			@RequestParam(required = false) Long buildingId,
			@RequestParam(required = false) Long employeeId,
			@RequestParam(defaultValue = "false") boolean unassigned) {
		return cleaningService.search(from, to, status, buildingId, employeeId, unassigned);
	}

	@GetMapping("/{id}")
	public CleaningResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		return cleaningService.findById(id, CurrentUser.from(jwt));
	}

	// Solo admin (ver SecurityConfig): reasignar empleado y franja de una limpieza pendiente
	@PatchMapping("/{id}")
	public CleaningResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCleaningRequest request) {
		return cleaningService.update(id, request);
	}

	@PatchMapping("/{id}/status")
	public CleaningResponse updateStatus(
			@PathVariable Long id,
			@Valid @RequestBody UpdateCleaningStatusRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return cleaningService.updateStatus(id, request.status(), CurrentUser.from(jwt));
	}

	@PatchMapping("/{id}/checklist/{itemId}")
	public CleaningResponse updateChecklistItem(
			@PathVariable Long id,
			@PathVariable Long itemId,
			@Valid @RequestBody UpdateChecklistItemRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return cleaningService.updateChecklistItem(id, itemId, request.done(), CurrentUser.from(jwt));
	}

	@PatchMapping("/{id}/incident")
	public CleaningResponse updateIncident(
			@PathVariable Long id,
			@Valid @RequestBody UpdateIncidentRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return cleaningService.updateIncident(id, request.incidentNotes(), CurrentUser.from(jwt));
	}
}
