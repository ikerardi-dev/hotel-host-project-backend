package com.hotelhost.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.BookingResponse;
import com.hotelhost.backend.dto.CreateBookingRequest;
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
}
