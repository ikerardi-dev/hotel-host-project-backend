package com.hotelhost.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.CreateEmployeeRequest;
import com.hotelhost.backend.dto.EmployeeResponse;
import com.hotelhost.backend.dto.EmployeeStatusResponse;
import com.hotelhost.backend.dto.TemporaryPasswordRequest;
import com.hotelhost.backend.dto.UpdateEmployeeRequest;
import com.hotelhost.backend.dto.UpdateEmployeeStatusRequest;
import com.hotelhost.backend.service.EmployeeService;

import jakarta.validation.Valid;

/**
 * Empleados de limpieza. Solo para administradores (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping
	public List<EmployeeResponse> findAll() {
		return employeeService.findAll();
	}

	@GetMapping("/{id}")
	public EmployeeResponse findById(@PathVariable Long id) {
		return employeeService.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
		return employeeService.create(request);
	}

	@PutMapping("/{id}")
	public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
		return employeeService.update(id, request);
	}

	@PutMapping("/{id}/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(@PathVariable Long id, @Valid @RequestBody TemporaryPasswordRequest request) {
		employeeService.resetPassword(id, request.temporaryPassword());
	}

	@PatchMapping("/{id}/status")
	public EmployeeStatusResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeStatusRequest request) {
		return employeeService.updateStatus(id, request.active());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		employeeService.delete(id);
	}
}
