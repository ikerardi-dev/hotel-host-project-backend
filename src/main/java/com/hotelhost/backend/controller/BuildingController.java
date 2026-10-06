package com.hotelhost.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.BuildingResponse;
import com.hotelhost.backend.service.BuildingService;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

	private final BuildingService buildingService;

	public BuildingController(BuildingService buildingService) {
		this.buildingService = buildingService;
	}

	@GetMapping
	public List<BuildingResponse> findAll() {
		return buildingService.findAll();
	}
}
