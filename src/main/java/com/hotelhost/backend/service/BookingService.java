package com.hotelhost.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.AvailabilityResponse;
import com.hotelhost.backend.dto.BookingResponse;
import com.hotelhost.backend.dto.CreateBookingRequest;
import com.hotelhost.backend.dto.CreateBookingRequest.CleaningPlan;
import com.hotelhost.backend.dto.UpdateBookingRequest;
import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingStatus;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningChecklistItem;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;

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
	private final EmployeeService employeeService;

	public BookingService(
			BookingRepository bookingRepository,
			ApartmentRepository apartmentRepository,
			CleaningRepository cleaningRepository,
			EmployeeService employeeService) {
		this.bookingRepository = bookingRepository;
		this.apartmentRepository = apartmentRepository;
		this.cleaningRepository = cleaningRepository;
		this.employeeService = employeeService;
	}

	/**
	 * Guarda la reserva y, si se pide, su limpieza de salida. Todo en la misma transacción.
	 */
	@Transactional
	public BookingResponse create(CreateBookingRequest request) {
		Apartment apartment = lockAndValidate(request.apartmentId(), request.guests(), request.checkIn(),
				request.checkOut(), null);

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

	/**
	 * Mismas validaciones que al crear. Si cambia la salida, la limpieza pendiente se mueve a la nueva fecha.
	 */
	@Transactional
	public BookingResponse update(Long id, UpdateBookingRequest request) {
		Booking booking = findActiveBooking(id);
		lockAndValidate(booking.getApartment().getId(), request.guests(), request.checkIn(), request.checkOut(), id);

		booking.setChannel(request.channel());
		booking.setGuestName(request.guestName().trim());
		booking.setGuestPhone(blankToNull(request.guestPhone()));
		booking.setGuestEmail(blankToNull(request.guestEmail()));
		booking.setReference(blankToNull(request.reference()));
		booking.setCheckIn(request.checkIn());
		booking.setCheckOut(request.checkOut());
		booking.setGuests(request.guests());
		booking.setNotes(blankToNull(request.notes()));

		Cleaning cleaning = activeCleaningOf(booking);
		if (cleaning != null && cleaning.getStatus() == CleaningStatus.PENDING) {
			cleaning.setCleaningDate(request.checkOut().toLocalDate());
		}
		return BookingResponse.from(booking, cleaning);
	}

	/**
	 * La reserva no se borra: se marca como cancelada, y su limpieza pendiente también se cancela.
	 */
	@Transactional
	public BookingResponse cancel(Long id) {
		Booking booking = findActiveBooking(id);
		booking.setStatus(BookingStatus.CANCELLED);

		Cleaning cleaning = activeCleaningOf(booking);
		if (cleaning != null && cleaning.getStatus() == CleaningStatus.PENDING) {
			cleaning.setStatus(CleaningStatus.CANCELLED);
		}
		return BookingResponse.from(booking, activeCleaningOf(booking));
	}

	/**
	 * Bloquea el apartamento (otra reserva simultánea del mismo apartamento espera a que acabe esta transacción)
	 * y comprueba fechas, plazas y solapamiento. excludeBookingId: la reserva que se edita (null al crear).
	 */
	private Apartment lockAndValidate(
			Long apartmentId, int guests, LocalDateTime checkIn, LocalDateTime checkOut, Long excludeBookingId) {
		if (!checkOut.isAfter(checkIn)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATES", "La salida debe ser posterior a la entrada");
		}

		Apartment apartment = apartmentRepository.findByIdForUpdate(apartmentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APARTMENT_NOT_FOUND", "El apartamento no existe"));

		if (guests > apartment.getCapacity()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "CAPACITY_EXCEEDED",
					"El apartamento tiene " + apartment.getCapacity() + " plazas como máximo");
		}
		if (bookingRepository.existsOverlapping(apartment.getId(), checkIn, checkOut, excludeBookingId)) {
			throw new ApiException(HttpStatus.CONFLICT, "BOOKING_OVERLAP",
					"Las fechas se solapan con otra reserva de este apartamento");
		}
		return apartment;
	}

	private Booking findActiveBooking(Long id) {
		Booking booking = bookingRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "La reserva no existe"));
		if (booking.getStatus() == BookingStatus.CANCELLED) {
			throw new ApiException(HttpStatus.CONFLICT, "BOOKING_CANCELLED", "La reserva está cancelada");
		}
		return booking;
	}

	private Cleaning activeCleaningOf(Booking booking) {
		return cleaningRepository.findByBookingId(booking.getId())
				.filter(cleaning -> cleaning.getStatus() != CleaningStatus.CANCELLED)
				.orElse(null);
	}

	// Calendario del apartamento: reservas activas que tocan el periodo, cada una con su limpieza
	@Transactional(readOnly = true)
	public List<BookingResponse> findByApartment(Long apartmentId, LocalDate from, LocalDate to) {
		DateRanges.validate(from, to);
		List<Booking> bookings = bookingRepository
				.findByApartmentIdAndStatusAndCheckInLessThanAndCheckOutGreaterThanOrderByCheckInAsc(
						apartmentId, BookingStatus.ACTIVE, to.plusDays(1).atStartOfDay(), from.atStartOfDay());

		Map<Long, Cleaning> cleaningByBooking = cleaningRepository
				.findByBookingIdIn(bookings.stream().map(Booking::getId).toList()).stream()
				.filter(cleaning -> cleaning.getStatus() != CleaningStatus.CANCELLED)
				.collect(Collectors.toMap(cleaning -> cleaning.getBooking().getId(), Function.identity()));

		return bookings.stream()
				.map(booking -> BookingResponse.from(booking, cleaningByBooking.get(booking.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	// excludeBookingId: al editar, la propia reserva no cuenta como ocupación
	public AvailabilityResponse availability(LocalDateTime checkIn, LocalDateTime checkOut, Long excludeBookingId) {
		if (!checkOut.isAfter(checkIn)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATES", "La salida debe ser posterior a la entrada");
		}
		return new AvailabilityResponse(
				bookingRepository.findApartmentIdsWithOverlapping(checkIn, checkOut, excludeBookingId));
	}

	private Cleaning scheduleCleaning(Booking booking, CleaningPlan plan) {
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(booking.getApartment());
		cleaning.setBooking(booking);
		cleaning.setCleaningDate(booking.getCheckOut().toLocalDate());
		cleaning.setStartTime(plan.startTime());
		cleaning.setEndTime(plan.endTime());
		cleaning.setAssignedTo(plan.cleanerId() == null ? null : employeeService.findActiveCleaner(plan.cleanerId()));

		for (int i = 0; i < DEFAULT_CHECKLIST.size(); i++) {
			cleaning.addChecklistItem(new CleaningChecklistItem(DEFAULT_CHECKLIST.get(i), i + 1));
		}
		return cleaningRepository.save(cleaning);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
