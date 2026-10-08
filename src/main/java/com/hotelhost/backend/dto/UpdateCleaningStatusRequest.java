package com.hotelhost.backend.dto;

import com.hotelhost.backend.entity.CleaningStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateCleaningStatusRequest(
		@NotNull(message = "Indica el nuevo estado de la limpieza")
		CleaningStatus status) {
}
