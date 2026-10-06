package com.hotelhost.backend.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.CreateEmployeeRequest;
import com.hotelhost.backend.dto.EmployeeResponse;
import com.hotelhost.backend.dto.EmployeeStatusResponse;
import com.hotelhost.backend.entity.Building;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.BuildingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;

/**
 * Gestión de los empleados de limpieza (solo administradores).
 */
@Service
public class EmployeeService {

	private final UserRepository userRepository;
	private final BuildingRepository buildingRepository;
	private final CleaningRepository cleaningRepository;
	private final PasswordEncoder passwordEncoder;

	public EmployeeService(
			UserRepository userRepository,
			BuildingRepository buildingRepository,
			CleaningRepository cleaningRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.buildingRepository = buildingRepository;
		this.cleaningRepository = cleaningRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<EmployeeResponse> findAll() {
		return userRepository.findByRoleOrderByFirstNameAscLastNameAsc(UserRole.CLEANING).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public EmployeeResponse create(CreateEmployeeRequest request) {
		String email = request.email().trim().toLowerCase();
		String dni = request.dni().trim().toUpperCase();

		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Ya existe un usuario con ese email");
		}
		if (userRepository.existsByDni(dni)) {
			throw new ApiException(HttpStatus.CONFLICT, "DNI_ALREADY_EXISTS", "Ya existe un empleado con ese DNI/NIE");
		}

		User employee = new User();
		employee.setFirstName(request.firstName().trim());
		employee.setLastName(request.lastName().trim());
		employee.setDni(dni);
		employee.setPhone(request.phone().trim());
		employee.setEmail(email);
		employee.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
		employee.setRole(UserRole.CLEANING);
		employee.setMustChangePassword(true);
		employee.setBuildings(findBuildings(request.buildingIds()));
		employee.setHireDate(request.hireDate());
		employee.setNotes(blankToNull(request.notes()));

		return toResponse(userRepository.save(employee));
	}

	@Transactional
	public EmployeeStatusResponse updateStatus(Long id, boolean active) {
		User employee = userRepository.findByIdAndRole(id, UserRole.CLEANING)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "El empleado no existe"));

		int unassignedCleanings = 0;
		if (employee.isActive() && !active) {
			unassignedCleanings = cleaningRepository.unassignPendingFrom(id, LocalDate.now());
		}
		employee.setActive(active);

		return new EmployeeStatusResponse(toResponse(employee), unassignedCleanings);
	}

	private Set<Building> findBuildings(Set<Long> ids) {
		List<Building> buildings = buildingRepository.findAllById(ids);
		if (buildings.size() != ids.size()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "BUILDING_NOT_FOUND", "Alguno de los edificios no existe");
		}
		return new HashSet<>(buildings);
	}

	private EmployeeResponse toResponse(User employee) {
		LocalDate today = LocalDate.now();
		LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate sunday = monday.plusDays(6);
		long cleaningsThisWeek = cleaningRepository.countByAssignedToIdAndCleaningDateBetween(employee.getId(), monday, sunday);
		return EmployeeResponse.from(employee, cleaningsThisWeek);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
