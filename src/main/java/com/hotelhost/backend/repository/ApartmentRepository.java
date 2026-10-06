package com.hotelhost.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hotelhost.backend.entity.Apartment;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

	// Con su edificio y sus camas en una sola consulta
	@Query("""
			SELECT DISTINCT a FROM Apartment a
			JOIN FETCH a.building b
			LEFT JOIN FETCH a.beds
			ORDER BY b.id, a.code
			""")
	List<Apartment> findAllWithBuildingAndBeds();
}
