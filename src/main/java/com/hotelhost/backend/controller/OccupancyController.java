package com.hotelhost.backend.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.OccupancyResponse;
import com.hotelhost.backend.service.OccupancyService;

/**
 * Planning de ocupación por edificio. Solo para administradores (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/occupancy")
public class OccupancyController {

	private final OccupancyService occupancyService;

	public OccupancyController(OccupancyService occupancyService) {
		this.occupancyService = occupancyService;
	}

	// Ejemplo: /api/occupancy?buildingId=2&from=2026-10-05&to=2026-10-11
	@GetMapping
	public OccupancyResponse occupancy(
			@RequestParam Long buildingId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
		return occupancyService.forBuilding(buildingId, from, to);
	}
}
