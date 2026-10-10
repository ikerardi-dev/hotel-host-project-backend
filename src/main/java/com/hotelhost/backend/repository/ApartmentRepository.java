package com.hotelhost.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelhost.backend.entity.Apartment;

import jakarta.persistence.LockModeType;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

	// Con su edificio y sus camas en una sola consulta
	@Query("""
			SELECT DISTINCT a FROM Apartment a
			JOIN FETCH a.building b
			LEFT JOIN FETCH a.beds
			ORDER BY b.id, a.code
			""")
	List<Apartment> findAllWithBuildingAndBeds();

	List<Apartment> findByBuildingIdOrderByCode(Long buildingId);

	// Bloquea la fila del apartamento hasta el final de la transacción (SELECT ... FOR UPDATE):
	// dos reservas simultáneas del mismo apartamento se comprueban de una en una
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Apartment a JOIN FETCH a.building WHERE a.id = :id")
	Optional<Apartment> findByIdForUpdate(@Param("id") Long id);
}
