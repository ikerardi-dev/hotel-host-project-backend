package com.hotelhost.backend.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.ApartmentResponse;
import com.hotelhost.backend.dto.AvailabilityResponse;
import com.hotelhost.backend.service.ApartmentService;
import com.hotelhost.backend.service.BookingService;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

	private final ApartmentService apartmentService;
	private final BookingService bookingService;

	public ApartmentController(ApartmentService apartmentService, BookingService bookingService) {
		this.apartmentService = apartmentService;
		this.bookingService = bookingService;
	}

	@GetMapping
	public List<ApartmentResponse> findAll() {
		return apartmentService.findAll();
	}

	// Ejemplo: /api/apartments/availability?checkIn=2026-10-20T16:00:00&checkOut=2026-10-25T11:00:00
	@GetMapping("/availability")
	public AvailabilityResponse availability(@RequestParam LocalDateTime checkIn, @RequestParam LocalDateTime checkOut) {
		return bookingService.availability(checkIn, checkOut);
	}
}
