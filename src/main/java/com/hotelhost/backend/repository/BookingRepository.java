package com.hotelhost.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	// Próxima entrada en un apartamento a partir de un momento
	Optional<Booking> findFirstByApartmentIdAndStatusAndCheckInGreaterThanEqualOrderByCheckInAsc(
			Long apartmentId, BookingStatus status, LocalDateTime from);

	// Entradas y salidas de un día: momento entre [start, end)
	List<Booking> findByStatusAndCheckInGreaterThanEqualAndCheckInLessThan(
			BookingStatus status, LocalDateTime start, LocalDateTime end);

	List<Booking> findByStatusAndCheckOutGreaterThanEqualAndCheckOutLessThan(
			BookingStatus status, LocalDateTime start, LocalDateTime end);

	// Reservas de un apartamento que tocan un periodo [from, to)
	List<Booking> findByApartmentIdAndStatusAndCheckInLessThanAndCheckOutGreaterThanOrderByCheckInAsc(
			Long apartmentId, BookingStatus status, LocalDateTime to, LocalDateTime from);

	// Reservas en curso o futuras (aún no han salido)
	List<Booking> findByStatusAndCheckOutAfterOrderByCheckInAsc(BookingStatus status, LocalDateTime moment);

	// Dos estancias se solapan si una empieza antes de que acabe la otra. Salida y entrada a la misma hora no se solapan.
	// excludeBookingId: la reserva que se está editando, para que no se solape consigo misma (null al crear)
	@Query("""
			SELECT COUNT(b) > 0 FROM Booking b
			WHERE b.apartment.id = :apartmentId
			  AND b.status = com.hotelhost.backend.entity.BookingStatus.ACTIVE
			  AND b.checkIn < :checkOut
			  AND b.checkOut > :checkIn
			  AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)
			""")
	boolean existsOverlapping(
			@Param("apartmentId") Long apartmentId,
			@Param("checkIn") LocalDateTime checkIn,
			@Param("checkOut") LocalDateTime checkOut,
			@Param("excludeBookingId") Long excludeBookingId);

	@Query("""
			SELECT DISTINCT b.apartment.id FROM Booking b
			WHERE b.status = com.hotelhost.backend.entity.BookingStatus.ACTIVE
			  AND b.checkIn < :checkOut
			  AND b.checkOut > :checkIn
			  AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)
			""")
	List<Long> findApartmentIdsWithOverlapping(
			@Param("checkIn") LocalDateTime checkIn,
			@Param("checkOut") LocalDateTime checkOut,
			@Param("excludeBookingId") Long excludeBookingId);
}
