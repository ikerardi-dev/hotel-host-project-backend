package com.hotelhost.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotelhost.backend.dto.BookingResponse;
import com.hotelhost.backend.dto.CreateBookingRequest;
import com.hotelhost.backend.dto.CreateBookingRequest.CleaningPlan;
import com.hotelhost.backend.dto.UpdateBookingRequest;
import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingChannel;
import com.hotelhost.backend.entity.BookingStatus;
import com.hotelhost.backend.entity.Building;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;

// Test unitario de la lógica de reservas: los repositorios son mocks, no hay base de datos
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

	private static final Long APARTMENT_ID = 10L;
	private static final Long BOOKING_ID = 1L;
	private static final LocalDateTime CHECK_IN = LocalDateTime.of(2030, 5, 10, 16, 0);
	private static final LocalDateTime CHECK_OUT = LocalDateTime.of(2030, 5, 14, 11, 0);

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private ApartmentRepository apartmentRepository;

	@Mock
	private CleaningRepository cleaningRepository;

	@Mock
	private EmployeeService employeeService;

	@InjectMocks
	private BookingService bookingService;

	private Apartment apartment;

	@BeforeEach
	void setUp() {
		// Legazpi B: 4 plazas
		Building building = mock(Building.class);
		lenient().when(building.getName()).thenReturn("Legazpi");
		apartment = mock(Apartment.class);
		lenient().when(apartment.getId()).thenReturn(APARTMENT_ID);
		lenient().when(apartment.getCode()).thenReturn("B");
		lenient().when(apartment.getCapacity()).thenReturn(4);
		lenient().when(apartment.getBuilding()).thenReturn(building);
	}

	@Test
	void checkOutBeforeCheckInIsRejectedBeforeTouchingTheDatabase() {
		assertThatThrownBy(() -> bookingService.create(request(CHECK_OUT, CHECK_IN, 2, null)))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("INVALID_DATES");
		verifyNoInteractions(apartmentRepository, bookingRepository);
	}

	@Test
	void moreGuestsThanCapacityIsRejected() {
		when(apartmentRepository.findByIdForUpdate(APARTMENT_ID)).thenReturn(Optional.of(apartment));

		assertThatThrownBy(() -> bookingService.create(request(CHECK_IN, CHECK_OUT, 5, null)))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("CAPACITY_EXCEEDED");
	}

	@Test
	void overlappingBookingIsRejectedAndNothingIsSaved() {
		when(apartmentRepository.findByIdForUpdate(APARTMENT_ID)).thenReturn(Optional.of(apartment));
		when(bookingRepository.existsOverlapping(APARTMENT_ID, CHECK_IN, CHECK_OUT, null)).thenReturn(true);

		assertThatThrownBy(() -> bookingService.create(request(CHECK_IN, CHECK_OUT, 2, null)))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("BOOKING_OVERLAP");
		verify(bookingRepository, never()).save(any());
		verify(cleaningRepository, never()).save(any());
	}

	@Test
	void creatingABookingSchedulesItsCleaningOnCheckOutDayWithTheChecklist() {
		when(apartmentRepository.findByIdForUpdate(APARTMENT_ID)).thenReturn(Optional.of(apartment));
		when(bookingRepository.existsOverlapping(APARTMENT_ID, CHECK_IN, CHECK_OUT, null)).thenReturn(false);
		when(cleaningRepository.save(any(Cleaning.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BookingResponse response = bookingService.create(
				request(CHECK_IN, CHECK_OUT, 3, new CleaningPlan(LocalTime.of(12, 0), LocalTime.of(14, 0), null)));

		ArgumentCaptor<Cleaning> saved = ArgumentCaptor.forClass(Cleaning.class);
		verify(cleaningRepository).save(saved.capture());
		Cleaning cleaning = saved.getValue();

		assertThat(cleaning.getCleaningDate()).isEqualTo(LocalDate.of(2030, 5, 14));
		assertThat(cleaning.getStatus()).isEqualTo(CleaningStatus.PENDING);
		assertThat(cleaning.getAssignedTo()).isNull();
		assertThat(cleaning.getChecklistItems()).hasSize(6);
		assertThat(response.guests()).isEqualTo(3);
		assertThat(response.cleaning().date()).isEqualTo(LocalDate.of(2030, 5, 14));
		verifyNoInteractions(employeeService);
	}

	@Test
	void bookingWithoutCleaningPlanDoesNotCreateACleaning() {
		when(apartmentRepository.findByIdForUpdate(APARTMENT_ID)).thenReturn(Optional.of(apartment));
		when(bookingRepository.existsOverlapping(APARTMENT_ID, CHECK_IN, CHECK_OUT, null)).thenReturn(false);

		BookingResponse response = bookingService.create(request(CHECK_IN, CHECK_OUT, 2, null));

		assertThat(response.cleaning()).isNull();
		verify(cleaningRepository, never()).save(any());
	}

	@Test
	void editingTheCheckOutMovesThePendingCleaning() {
		Booking booking = activeBooking();
		Cleaning cleaning = cleaning(CleaningStatus.PENDING, LocalDate.of(2030, 5, 14));
		LocalDateTime newCheckOut = LocalDateTime.of(2030, 5, 16, 11, 0);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		when(apartmentRepository.findByIdForUpdate(APARTMENT_ID)).thenReturn(Optional.of(apartment));
		when(bookingRepository.existsOverlapping(APARTMENT_ID, CHECK_IN, newCheckOut, BOOKING_ID)).thenReturn(false);
		when(cleaningRepository.findByBookingId(any())).thenReturn(Optional.of(cleaning));

		bookingService.update(BOOKING_ID, update(CHECK_IN, newCheckOut));

		assertThat(booking.getCheckOut()).isEqualTo(newCheckOut);
		assertThat(cleaning.getCleaningDate()).isEqualTo(LocalDate.of(2030, 5, 16));
	}

	@Test
	void editingDoesNotMoveACleaningThatHasAlreadyStarted() {
		Booking booking = activeBooking();
		Cleaning cleaning = cleaning(CleaningStatus.IN_PROGRESS, LocalDate.of(2030, 5, 14));
		LocalDateTime newCheckOut = LocalDateTime.of(2030, 5, 16, 11, 0);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		when(apartmentRepository.findByIdForUpdate(APARTMENT_ID)).thenReturn(Optional.of(apartment));
		when(bookingRepository.existsOverlapping(APARTMENT_ID, CHECK_IN, newCheckOut, BOOKING_ID)).thenReturn(false);
		when(cleaningRepository.findByBookingId(any())).thenReturn(Optional.of(cleaning));

		bookingService.update(BOOKING_ID, update(CHECK_IN, newCheckOut));

		assertThat(cleaning.getCleaningDate()).isEqualTo(LocalDate.of(2030, 5, 14));
	}

	@Test
	void cancellingCancelsTheBookingAndItsPendingCleaning() {
		Booking booking = activeBooking();
		Cleaning cleaning = cleaning(CleaningStatus.PENDING, LocalDate.of(2030, 5, 14));
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		when(cleaningRepository.findByBookingId(any())).thenReturn(Optional.of(cleaning));

		BookingResponse response = bookingService.cancel(BOOKING_ID);

		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(cleaning.getStatus()).isEqualTo(CleaningStatus.CANCELLED);
		assertThat(response.cleaning()).isNull();
	}

	@Test
	void aCancelledBookingCannotBeCancelledOrEditedAgain() {
		Booking booking = activeBooking();
		booking.setStatus(BookingStatus.CANCELLED);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

		assertThatThrownBy(() -> bookingService.cancel(BOOKING_ID))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("BOOKING_CANCELLED");
		assertThatThrownBy(() -> bookingService.update(BOOKING_ID, update(CHECK_IN, CHECK_OUT)))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("BOOKING_CANCELLED");
	}

	private Booking activeBooking() {
		Booking booking = new Booking();
		booking.setApartment(apartment);
		booking.setGuestName("Laura Gómez");
		booking.setChannel(BookingChannel.BOOKING);
		booking.setCheckIn(CHECK_IN);
		booking.setCheckOut(CHECK_OUT);
		booking.setGuests(2);
		return booking;
	}

	private Cleaning cleaning(CleaningStatus status, LocalDate date) {
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(apartment);
		cleaning.setCleaningDate(date);
		cleaning.setStartTime(LocalTime.of(12, 0));
		cleaning.setEndTime(LocalTime.of(14, 0));
		cleaning.setStatus(status);
		return cleaning;
	}

	private static CreateBookingRequest request(LocalDateTime checkIn, LocalDateTime checkOut, int guests, CleaningPlan plan) {
		return new CreateBookingRequest(APARTMENT_ID, BookingChannel.BOOKING, "Laura Gómez", "654987321", null, "BK-1",
				checkIn, checkOut, guests, null, plan);
	}

	private static UpdateBookingRequest update(LocalDateTime checkIn, LocalDateTime checkOut) {
		return new UpdateBookingRequest(BookingChannel.AIRBNB, "Laura Gómez", null, null, null, checkIn, checkOut, 2, null);
	}
}
