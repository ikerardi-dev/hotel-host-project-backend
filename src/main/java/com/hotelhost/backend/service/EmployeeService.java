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
import com.hotelhost.backend.dto.UpdateEmployeeRequest;
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

	@Transactional(readOnly = true)
	public EmployeeResponse findById(Long id) {
		return toResponse(findEmployee(id));
	}

	@Transactional
	public EmployeeResponse create(CreateEmployeeRequest request) {
		String email = request.email().trim().toLowerCase();
		String dni = request.dni().trim().toUpperCase();

		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw emailAlreadyExists();
		}
		if (userRepository.existsByDni(dni)) {
			throw dniAlreadyExists();
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
	public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
		User employee = findEmployee(id);
		String email = request.email().trim().toLowerCase();
		String dni = request.dni().trim().toUpperCase();

		if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
			throw emailAlreadyExists();
		}
		if (userRepository.existsByDniAndIdNot(dni, id)) {
			throw dniAlreadyExists();
		}

		employee.setFirstName(request.firstName().trim());
		employee.setLastName(request.lastName().trim());
		employee.setDni(dni);
		employee.setPhone(request.phone().trim());
		employee.setEmail(email);
		employee.setBuildings(findBuildings(request.buildingIds()));
		employee.setHireDate(request.hireDate());
		employee.setNotes(blankToNull(request.notes()));

		return toResponse(employee);
	}

	// Contraseña olvidada: la anterior deja de valer y en el próximo acceso tendrá que cambiar la temporal
	@Transactional
	public void resetPassword(Long id, String temporaryPassword) {
		User employee = findEmployee(id);
		employee.setPasswordHash(passwordEncoder.encode(temporaryPassword));
		employee.setMustChangePassword(true);
	}

	@Transactional
	public EmployeeStatusResponse updateStatus(Long id, boolean active) {
		User employee = findEmployee(id);

		int unassignedCleanings = 0;
		if (employee.isActive() && !active) {
			unassignedCleanings = cleaningRepository.unassignPendingFrom(id, LocalDate.now());
		}
		employee.setActive(active);

		return new EmployeeStatusResponse(toResponse(employee), unassignedCleanings);
	}

	// Borrado definitivo, solo de empleados ya dados de baja. Su historial de limpiezas conserva el nombre.
	@Transactional
	public void delete(Long id) {
		User employee = findEmployee(id);
		if (employee.isActive()) {
			throw new ApiException(HttpStatus.CONFLICT, "EMPLOYEE_ACTIVE",
					"Da de baja al empleado antes de eliminarlo");
		}

		cleaningRepository.detachFromEmployee(id, employee.getFirstName() + " " + employee.getLastName());
		userRepository.deleteById(id);
	}

	// Empleado al que se puede asignar una limpieza: de limpieza y activo
	@Transactional(readOnly = true)
	public User findActiveCleaner(Long id) {
		return userRepository.findByIdAndRole(id, UserRole.CLEANING)
				.filter(User::isActive)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CLEANER",
						"El empleado asignado no existe o está dado de baja"));
	}

	private User findEmployee(Long id) {
		return userRepository.findByIdAndRole(id, UserRole.CLEANING)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "El empleado no existe"));
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

	private static ApiException emailAlreadyExists() {
		return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Ya existe un usuario con ese email");
	}

	private static ApiException dniAlreadyExists() {
		return new ApiException(HttpStatus.CONFLICT, "DNI_ALREADY_EXISTS", "Ya existe un empleado con ese DNI/NIE");
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
