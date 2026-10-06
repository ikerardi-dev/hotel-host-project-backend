package com.hotelhost.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hotelhost.backend.dto.CreateEmployeeRequest;
import com.hotelhost.backend.dto.EmployeeResponse;
import com.hotelhost.backend.dto.EmployeeStatusResponse;
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

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
		return employeeService.create(request);
	}

	@PatchMapping("/{id}/status")
	public EmployeeStatusResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeStatusRequest request) {
		return employeeService.updateStatus(id, request.active());
	}
}
