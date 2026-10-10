package com.hotelhost.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.TodayDashboardResponse;
import com.hotelhost.backend.service.DashboardService;

/**
 * Panel "Hoy" del admin (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/today")
	public TodayDashboardResponse today() {
		return dashboardService.today();
	}
}
