package com.hotelhost.backend.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningStatus;

public interface CleaningRepository extends JpaRepository<Cleaning, Long> {

	long countByAssignedToIdAndCleaningDateBetween(Long userId, LocalDate from, LocalDate to);

	List<Cleaning> findByCleaningDateLessThanEqualAndStatusIn(LocalDate date, Collection<CleaningStatus> statuses);

	List<Cleaning> findByCleaningDateAndStatus(LocalDate date, CleaningStatus status);

	List<Cleaning> findByBookingIdIn(Collection<Long> bookingIds);

	Optional<Cleaning> findByBookingId(Long bookingId);

	List<Cleaning> findByCleaningDateAndStatusNot(LocalDate date, CleaningStatus excludedStatus);

	List<Cleaning> findByApartmentBuildingIdAndCleaningDateBetweenAndStatusNot(
			Long buildingId, LocalDate from, LocalDate to, CleaningStatus excludedStatus);

	List<Cleaning> findByApartmentIdAndCleaningDateBetweenAndStatusNotOrderByCleaningDateAscStartTimeAsc(
			Long apartmentId, LocalDate from, LocalDate to, CleaningStatus excludedStatus);

	// Limpiezas de un empleado desde hoy, más las atrasadas sin terminar (sin las canceladas)
	@Query("""
			SELECT c FROM Cleaning c
			JOIN FETCH c.apartment a
			JOIN FETCH a.building
			WHERE c.assignedTo.id = :userId
			  AND c.status <> com.hotelhost.backend.entity.CleaningStatus.CANCELLED
			  AND (c.cleaningDate >= :today
			       OR c.status IN (com.hotelhost.backend.entity.CleaningStatus.PENDING,
			                       com.hotelhost.backend.entity.CleaningStatus.IN_PROGRESS))
			ORDER BY c.cleaningDate, c.startTime
			""")
	List<Cleaning> findCurrentByAssignedTo(@Param("userId") Long userId, @Param("today") LocalDate today);

	// Control del admin: limpiezas de un periodo con filtros opcionales (null = sin filtrar)
	@Query("""
			SELECT c FROM Cleaning c
			JOIN FETCH c.apartment a
			JOIN FETCH a.building b
			LEFT JOIN FETCH c.assignedTo u
			WHERE c.cleaningDate BETWEEN :from AND :to
			  AND (:status IS NULL OR c.status = :status)
			  AND (:buildingId IS NULL OR b.id = :buildingId)
			  AND (:employeeId IS NULL OR u.id = :employeeId)
			  AND (:unassigned = FALSE OR u IS NULL)
			ORDER BY c.cleaningDate, c.startTime
			""")
	List<Cleaning> search(
			@Param("from") LocalDate from,
			@Param("to") LocalDate to,
			@Param("status") CleaningStatus status,
			@Param("buildingId") Long buildingId,
			@Param("employeeId") Long employeeId,
			@Param("unassigned") boolean unassigned);

	// Al dar de baja a un empleado: sus limpiezas pendientes desde hoy se quedan sin asignar
	@Modifying
	@Query("""
			UPDATE Cleaning c SET c.assignedTo = NULL
			WHERE c.assignedTo.id = :userId
			  AND c.status = com.hotelhost.backend.entity.CleaningStatus.PENDING
			  AND c.cleaningDate >= :from
			""")
	int unassignPendingFrom(@Param("userId") Long userId, @Param("from") LocalDate from);
}
