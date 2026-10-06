package com.hotelhost.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.ApartmentResponse;
import com.hotelhost.backend.service.ApartmentService;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

	private final ApartmentService apartmentService;

	public ApartmentController(ApartmentService apartmentService) {
		this.apartmentService = apartmentService;
	}

	@GetMapping
	public List<ApartmentResponse> findAll() {
		return apartmentService.findAll();
	}
}
