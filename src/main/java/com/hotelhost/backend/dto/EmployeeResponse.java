package com.hotelhost.backend.dto;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.hotelhost.backend.entity.User;

public record EmployeeResponse(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phone,
		String dni,
		List<BuildingSummary> buildings,
		boolean active,
		LocalDate hireDate,
		String notes,
		long cleaningsThisWeek) {

	public record BuildingSummary(Long id, String name) {
	}

	public static EmployeeResponse from(User user, long cleaningsThisWeek) {
		List<BuildingSummary> buildings = user.getBuildings().stream()
				.sorted(Comparator.comparing(building -> building.getId()))
				.map(building -> new BuildingSummary(building.getId(), building.getName()))
				.toList();

		return new EmployeeResponse(
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getEmail(),
				user.getPhone(),
				user.getDni(),
				buildings,
				user.isActive(),
				user.getHireDate(),
				user.getNotes(),
				cleaningsThisWeek);
	}
}
