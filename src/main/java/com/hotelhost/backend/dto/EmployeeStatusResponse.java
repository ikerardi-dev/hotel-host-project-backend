package com.hotelhost.backend.dto;

/**
 * unassignedCleanings: limpiezas futuras que se han quedado sin asignar al dar de baja al empleado.
 */
public record EmployeeStatusResponse(EmployeeResponse employee, int unassignedCleanings) {
}
