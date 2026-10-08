package com.hotelhost.backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.ApartmentResponse;
import com.hotelhost.backend.dto.AvailabilityResponse;
import com.hotelhost.backend.dto.BookingResponse;
import com.hotelhost.backend.dto.CleaningResponse;
import com.hotelhost.backend.service.ApartmentService;
import com.hotelhost.backend.service.BookingService;
import com.hotelhost.backend.service.CleaningService;

/**
 * Apartamentos, su disponibilidad y su calendario. Solo para administradores (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

	private final ApartmentService apartmentService;
	private final BookingService bookingService;
	private final CleaningService cleaningService;

	public ApartmentController(
			ApartmentService apartmentService,
			BookingService bookingService,
			CleaningService cleaningService) {
		this.apartmentService = apartmentService;
		this.bookingService = bookingService;
		this.cleaningService = cleaningService;
	}

	@GetMapping
	public List<ApartmentResponse> findAll() {
		return apartmentService.findAll();
	}

	// Ejemplo: /api/apartments/availability?checkIn=2026-10-20T16:00:00&checkOut=2026-10-25T11:00:00
	@GetMapping("/availability")
	// excludeBookingId (opcional): al editar una reserva, para que no cuente como ocupación
	public AvailabilityResponse availability(
			@RequestParam LocalDateTime checkIn,
			@RequestParam LocalDateTime checkOut,
			@RequestParam(required = false) Long excludeBookingId) {
		return bookingService.availability(checkIn, checkOut, excludeBookingId);
	}

	@GetMapping("/{id}")
	public ApartmentResponse findById(@PathVariable Long id) {
		return apartmentService.findById(id);
	}

	// Ejemplo: /api/apartments/5/bookings?from=2026-10-01&to=2026-10-31
	@GetMapping("/{id}/bookings")
	public List<BookingResponse> bookings(
			@PathVariable Long id, @RequestParam LocalDate from, @RequestParam LocalDate to) {
		return bookingService.findByApartment(id, from, to);
	}

	@GetMapping("/{id}/cleanings")
	public List<CleaningResponse> cleanings(
			@PathVariable Long id, @RequestParam LocalDate from, @RequestParam LocalDate to) {
		return cleaningService.findByApartment(id, from, to);
	}
}
