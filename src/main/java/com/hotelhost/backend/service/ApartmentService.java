package com.hotelhost.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.ApartmentResponse;
import com.hotelhost.backend.dto.ApartmentResponse.ApartmentStatus;
import com.hotelhost.backend.dto.ApartmentResponse.BedSummary;
import com.hotelhost.backend.dto.ApartmentResponse.BuildingSummary;
import com.hotelhost.backend.dto.ApartmentResponse.GuestSummary;
import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingStatus;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;

@Service
public class ApartmentService {

	private final ApartmentRepository apartmentRepository;
	private final BookingRepository bookingRepository;
	private final CleaningRepository cleaningRepository;

	public ApartmentService(
			ApartmentRepository apartmentRepository,
			BookingRepository bookingRepository,
			CleaningRepository cleaningRepository) {
		this.apartmentRepository = apartmentRepository;
		this.bookingRepository = bookingRepository;
		this.cleaningRepository = cleaningRepository;
	}

	/**
	 * Todos los apartamentos con su estado. Prioridad del estado:
	 * ocupado > pendiente de limpieza > limpio y listo (limpieza terminada hoy) > libre.
	 */
	@Transactional(readOnly = true)
	public List<ApartmentResponse> findAll() {
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = now.toLocalDate();

		// Reservas en curso o futuras, por apartamento y ordenadas por entrada
		Map<Long, List<Booking>> bookingsByApartment = bookingRepository
				.findByStatusAndCheckOutAfterOrderByCheckInAsc(BookingStatus.ACTIVE, now).stream()
				.collect(Collectors.groupingBy(booking -> booking.getApartment().getId()));

		Set<Long> pendingCleaning = apartmentIds(cleaningRepository.findByCleaningDateLessThanEqualAndStatusIn(
				today, List.of(CleaningStatus.PENDING, CleaningStatus.IN_PROGRESS)));
		Set<Long> cleanedToday = apartmentIds(cleaningRepository.findByCleaningDateAndStatus(today, CleaningStatus.DONE));

		return apartmentRepository.findAllWithBuildingAndBeds().stream()
				.map(apartment -> toResponse(apartment, bookingsByApartment.getOrDefault(apartment.getId(), List.of()),
						pendingCleaning, cleanedToday, now))
				.toList();
	}

	private ApartmentResponse toResponse(
			Apartment apartment,
			List<Booking> bookings,
			Set<Long> pendingCleaning,
			Set<Long> cleanedToday,
			LocalDateTime now) {
		Booking current = bookings.stream()
				.filter(booking -> !booking.getCheckIn().isAfter(now))
				.findFirst()
				.orElse(null);
		Booking next = bookings.stream()
				.filter(booking -> booking.getCheckIn().isAfter(now))
				.findFirst()
				.orElse(null);

		ApartmentStatus status;
		if (current != null) {
			status = ApartmentStatus.OCCUPIED;
		} else if (pendingCleaning.contains(apartment.getId())) {
			status = ApartmentStatus.PENDING_CLEANING;
		} else if (cleanedToday.contains(apartment.getId())) {
			status = ApartmentStatus.READY;
		} else {
			status = ApartmentStatus.FREE;
		}

		List<BedSummary> beds = apartment.getBeds().stream()
				.sorted(Comparator.comparing(bed -> bed.getType().ordinal()))
				.map(bed -> new BedSummary(bed.getType(), bed.getQuantity()))
				.toList();

		return new ApartmentResponse(
				apartment.getId(),
				apartment.getCode(),
				apartment.getCapacity(),
				new BuildingSummary(apartment.getBuilding().getId(), apartment.getBuilding().getName()),
				beds,
				status,
				guest(current),
				guest(next));
	}

	private static GuestSummary guest(Booking booking) {
		return booking == null
				? null
				: new GuestSummary(booking.getId(), booking.getGuestName(), booking.getCheckIn(), booking.getCheckOut());
	}

	private static Set<Long> apartmentIds(List<Cleaning> cleanings) {
		return cleanings.stream().map(cleaning -> cleaning.getApartment().getId()).collect(Collectors.toSet());
	}
}
