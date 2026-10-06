package com.hotelhost.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	// Reservas en curso o futuras (aún no han salido)
	List<Booking> findByStatusAndCheckOutAfterOrderByCheckInAsc(BookingStatus status, LocalDateTime moment);
}
