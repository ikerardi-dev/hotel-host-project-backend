package com.hotelhost.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.BookingResponse;
import com.hotelhost.backend.dto.CreateBookingRequest;
import com.hotelhost.backend.dto.UpdateBookingRequest;
import com.hotelhost.backend.service.BookingService;

import jakarta.validation.Valid;

/**
 * Reservas anotadas a mano. Solo para administradores (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

	private final BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
		return bookingService.create(request);
	}

	@PutMapping("/{id}")
	public BookingResponse update(@PathVariable Long id, @Valid @RequestBody UpdateBookingRequest request) {
		return bookingService.update(id, request);
	}

	@PatchMapping("/{id}/cancel")
	public BookingResponse cancel(@PathVariable Long id) {
		return bookingService.cancel(id);
	}
}
