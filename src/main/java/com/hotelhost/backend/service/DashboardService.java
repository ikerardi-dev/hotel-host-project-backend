package com.hotelhost.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.ApartmentResponse;
import com.hotelhost.backend.dto.ApartmentResponse.ApartmentStatus;
import com.hotelhost.backend.dto.TodayDashboardResponse;
import com.hotelhost.backend.dto.TodayDashboardResponse.EventType;
import com.hotelhost.backend.dto.TodayDashboardResponse.TimelineEvent;
import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingStatus;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;

@Service
public class DashboardService {

	private final BookingRepository bookingRepository;
	private final CleaningRepository cleaningRepository;
	private final ApartmentService apartmentService;

	public DashboardService(
			BookingRepository bookingRepository,
			CleaningRepository cleaningRepository,
			ApartmentService apartmentService) {
		this.bookingRepository = bookingRepository;
		this.cleaningRepository = cleaningRepository;
		this.apartmentService = apartmentService;
	}

	@Transactional(readOnly = true)
	public TodayDashboardResponse today() {
		LocalDate today = LocalDate.now();
		LocalDateTime start = today.atStartOfDay();
		LocalDateTime end = today.plusDays(1).atStartOfDay();

		List<Booking> checkIns = bookingRepository
				.findByStatusAndCheckInGreaterThanEqualAndCheckInLessThan(BookingStatus.ACTIVE, start, end);
		List<Booking> checkOuts = bookingRepository
				.findByStatusAndCheckOutGreaterThanEqualAndCheckOutLessThan(BookingStatus.ACTIVE, start, end);
		List<Cleaning> cleanings = cleaningRepository.findByCleaningDateAndStatusNot(today, CleaningStatus.CANCELLED);
		List<ApartmentResponse> apartments = apartmentService.findAll();

		List<TimelineEvent> timeline = new ArrayList<>();
		checkOuts.forEach(booking -> timeline.add(bookingEvent(EventType.CHECK_OUT, booking, booking.getCheckOut())));
		cleanings.forEach(cleaning -> timeline.add(cleaningEvent(cleaning)));
		checkIns.forEach(booking -> timeline.add(bookingEvent(EventType.CHECK_IN, booking, booking.getCheckIn())));
		// A la misma hora: primero la salida, luego la limpieza y después la entrada (orden del enum)
		timeline.sort(Comparator.comparing(TimelineEvent::time).thenComparing(TimelineEvent::type));

		return new TodayDashboardResponse(
				checkIns.size(),
				checkOuts.size(),
				countByStatus(cleanings, CleaningStatus.PENDING),
				countByStatus(cleanings, CleaningStatus.IN_PROGRESS),
				countByStatus(cleanings, CleaningStatus.DONE),
				(int) apartments.stream().filter(apartment -> apartment.status() == ApartmentStatus.OCCUPIED).count(),
				apartments.size(),
				timeline);
	}

	private static TimelineEvent bookingEvent(EventType type, Booking booking, LocalDateTime moment) {
		Apartment apartment = booking.getApartment();
		return new TimelineEvent(type, moment.toLocalTime(), apartment.getId(), apartment.getCode(),
				apartment.getBuilding().getName(), booking.getGuestName(), null, null, null, null);
	}

	private static TimelineEvent cleaningEvent(Cleaning cleaning) {
		Apartment apartment = cleaning.getApartment();
		String assignedTo = cleaning.getAssignedTo() == null
				? null
				: cleaning.getAssignedTo().getFirstName() + " " + cleaning.getAssignedTo().getLastName();
		return new TimelineEvent(EventType.CLEANING, cleaning.getStartTime(), apartment.getId(), apartment.getCode(),
				apartment.getBuilding().getName(), null, cleaning.getId(), cleaning.getEndTime(), assignedTo,
				cleaning.getStatus());
	}

	private static int countByStatus(List<Cleaning> cleanings, CleaningStatus status) {
		return (int) cleanings.stream().filter(cleaning -> cleaning.getStatus() == status).count();
	}
}
