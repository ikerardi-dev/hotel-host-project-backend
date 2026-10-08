package com.hotelhost.backend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;

import com.hotelhost.backend.exception.ApiException;

/**
 * Periodos de consulta (calendario, control de limpiezas): como mucho 62 días, unos dos meses.
 */
final class DateRanges {

	static final int MAX_DAYS = 62;

	private DateRanges() {
	}

	static void validate(LocalDate from, LocalDate to) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
					"El periodo debe ser de " + MAX_DAYS + " días como máximo y la fecha final no puede ser anterior");
		}
	}
}
