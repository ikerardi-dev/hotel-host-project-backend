package com.hotelhost.backend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateChecklistItemRequest(
		@NotNull(message = "Indica si la tarea está hecha")
		Boolean done) {
}
