package com.hotelhost.backend.dto;

import java.util.List;

/**
 * Apartamentos que ya tienen una reserva que se solapa con las fechas consultadas.
 */
public record AvailabilityResponse(List<Long> unavailableApartmentIds) {
}
