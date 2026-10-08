package com.hotelhost.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Incidencia en texto (vacía o null para borrarla).
 */
public record UpdateIncidentRequest(
		@Size(max = 1000, message = "La incidencia no puede superar los 1000 caracteres")
		String incidentNotes) {
}
