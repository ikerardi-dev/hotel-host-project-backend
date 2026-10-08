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

	// Reservas en curso o futuras (aún no han salido)
	List<Booking> findByStatusAndCheckOutAfterOrderByCheckInAsc(BookingStatus status, LocalDateTime moment);

	// Dos estancias se solapan si una empieza antes de que acabe la otra. Salida y entrada a la misma hora no se solapan.
	@Query("""
			SELECT COUNT(b) > 0 FROM Booking b
			WHERE b.apartment.id = :apartmentId
			  AND b.status = com.hotelhost.backend.entity.BookingStatus.ACTIVE
			  AND b.checkIn < :checkOut
			  AND b.checkOut > :checkIn
			""")
	boolean existsOverlapping(
			@Param("apartmentId") Long apartmentId,
			@Param("checkIn") LocalDateTime checkIn,
			@Param("checkOut") LocalDateTime checkOut);

	@Query("""
			SELECT DISTINCT b.apartment.id FROM Booking b
			WHERE b.status = com.hotelhost.backend.entity.BookingStatus.ACTIVE
			  AND b.checkIn < :checkOut
			  AND b.checkOut > :checkIn
			""")
	List<Long> findApartmentIdsWithOverlapping(
			@Param("checkIn") LocalDateTime checkIn,
			@Param("checkOut") LocalDateTime checkOut);
}
