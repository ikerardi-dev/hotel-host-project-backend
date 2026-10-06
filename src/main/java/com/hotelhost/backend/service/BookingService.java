package com.hotelhost.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.AvailabilityResponse;
import com.hotelhost.backend.dto.BookingResponse;
import com.hotelhost.backend.dto.CreateBookingRequest;
import com.hotelhost.backend.dto.CreateBookingRequest.CleaningPlan;
import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningChecklistItem;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;

@Service
public class BookingService {

	// Tareas de la checklist de cada limpieza nueva, en este orden
	static final List<String> DEFAULT_CHECKLIST = List.of(
			"Cambiar sábanas",
			"Toallas",
			"Baño",
			"Cocina",
			"Reponer amenities",
			"Revisar desperfectos");

	private final BookingRepository bookingRepository;
	private final ApartmentRepository apartmentRepository;
	private final CleaningRepository cleaningRepository;
	private final UserRepository userRepository;

	public BookingService(
			BookingRepository bookingRepository,
			ApartmentRepository apartmentRepository,
			CleaningRepository cleaningRepository,
			UserRepository userRepository) {
		this.bookingRepository = bookingRepository;
		this.apartmentRepository = apartmentRepository;
		this.cleaningRepository = cleaningRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Guarda la reserva y, si se pide, su limpieza de salida. Todo en la misma transacción.
	 */
	@Transactional
	public BookingResponse create(CreateBookingRequest request) {
		if (!request.checkOut().isAfter(request.checkIn())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATES", "La salida debe ser posterior a la entrada");
		}

		// Bloquea el apartamento: otra reserva simultánea del mismo apartamento espera a que acabe esta
		Apartment apartment = apartmentRepository.findByIdForUpdate(request.apartmentId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APARTMENT_NOT_FOUND", "El apartamento no existe"));

		if (request.guests() > apartment.getCapacity()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "CAPACITY_EXCEEDED",
					"El apartamento tiene " + apartment.getCapacity() + " plazas como máximo");
		}
		if (bookingRepository.existsOverlapping(apartment.getId(), request.checkIn(), request.checkOut())) {
			throw new ApiException(HttpStatus.CONFLICT, "BOOKING_OVERLAP",
					"Las fechas se solapan con otra reserva de este apartamento");
		}

		Booking booking = new Booking();
		booking.setApartment(apartment);
		booking.setChannel(request.channel());
		booking.setGuestName(request.guestName().trim());
		booking.setGuestPhone(blankToNull(request.guestPhone()));
		booking.setGuestEmail(blankToNull(request.guestEmail()));
		booking.setReference(blankToNull(request.reference()));
		booking.setCheckIn(request.checkIn());
		booking.setCheckOut(request.checkOut());
		booking.setGuests(request.guests());
		booking.setNotes(blankToNull(request.notes()));
		bookingRepository.save(booking);

		Cleaning cleaning = request.cleaning() == null ? null : scheduleCleaning(booking, request.cleaning());

		return BookingResponse.from(booking, cleaning);
	}

	@Transactional(readOnly = true)
	public AvailabilityResponse availability(LocalDateTime checkIn, LocalDateTime checkOut) {
		if (!checkOut.isAfter(checkIn)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATES", "La salida debe ser posterior a la entrada");
		}
		return new AvailabilityResponse(bookingRepository.findApartmentIdsWithOverlapping(checkIn, checkOut));
	}

	private Cleaning scheduleCleaning(Booking booking, CleaningPlan plan) {
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(booking.getApartment());
		cleaning.setBooking(booking);
		cleaning.setCleaningDate(booking.getCheckOut().toLocalDate());
		cleaning.setStartTime(plan.startTime());
		cleaning.setEndTime(plan.endTime());
		cleaning.setAssignedTo(plan.cleanerId() == null ? null : findActiveCleaner(plan.cleanerId()));

		for (int i = 0; i < DEFAULT_CHECKLIST.size(); i++) {
			cleaning.addChecklistItem(new CleaningChecklistItem(DEFAULT_CHECKLIST.get(i), i + 1));
		}
		return cleaningRepository.save(cleaning);
	}

	private User findActiveCleaner(Long id) {
		return userRepository.findByIdAndRole(id, UserRole.CLEANING)
				.filter(User::isActive)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CLEANER",
						"El empleado asignado no existe o está dado de baja"));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
