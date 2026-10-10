package com.hotelhost.backend.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.OccupancyResponse;
import com.hotelhost.backend.dto.OccupancyResponse.ApartmentRow;
import com.hotelhost.backend.dto.OccupancyResponse.BookingBar;
import com.hotelhost.backend.dto.OccupancyResponse.CleaningMark;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;

@Service
public class OccupancyService {

	private final ApartmentRepository apartmentRepository;
	private final BookingRepository bookingRepository;
	private final CleaningRepository cleaningRepository;

	public OccupancyService(
			ApartmentRepository apartmentRepository,
			BookingRepository bookingRepository,
			CleaningRepository cleaningRepository) {
		this.apartmentRepository = apartmentRepository;
		this.bookingRepository = bookingRepository;
		this.cleaningRepository = cleaningRepository;
	}

	/**
	 * Todo lo que necesita el planning de un edificio en una sola consulta por tipo de dato.
	 */
	@Transactional(readOnly = true)
	public OccupancyResponse forBuilding(Long buildingId, LocalDate from, LocalDate to) {
		DateRanges.validate(from, to);

		var apartments = apartmentRepository.findByBuildingIdOrderByCode(buildingId).stream()
				.map(apartment -> new ApartmentRow(apartment.getId(), apartment.getCode(), apartment.getCapacity()))
				.toList();

		var bookings = bookingRepository
				.findActiveInBuilding(buildingId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
				.map(booking -> new BookingBar(booking.getId(), booking.getApartment().getId(), booking.getGuestName(),
						booking.getChannel(), booking.getCheckIn(), booking.getCheckOut()))
				.toList();

		var cleanings = cleaningRepository
				.findByApartmentBuildingIdAndCleaningDateBetweenAndStatusNot(buildingId, from, to, CleaningStatus.CANCELLED)
				.stream()
				.map(cleaning -> new CleaningMark(cleaning.getId(), cleaning.getApartment().getId(),
						cleaning.getCleaningDate(), cleaning.getStatus()))
				.toList();

		return new OccupancyResponse(apartments, bookings, cleanings);
	}
}
