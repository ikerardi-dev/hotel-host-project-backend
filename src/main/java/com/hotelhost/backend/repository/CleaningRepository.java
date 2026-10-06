package com.hotelhost.backend.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelhost.backend.entity.Cleaning;

public interface CleaningRepository extends JpaRepository<Cleaning, Long> {

	long countByAssignedToIdAndCleaningDateBetween(Long userId, LocalDate from, LocalDate to);

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
