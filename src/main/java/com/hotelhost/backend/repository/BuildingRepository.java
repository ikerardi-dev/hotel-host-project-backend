package com.hotelhost.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hotelhost.backend.dto.BuildingResponse;
import com.hotelhost.backend.entity.Building;

public interface BuildingRepository extends JpaRepository<Building, Long> {

	@Query("""
			SELECT new com.hotelhost.backend.dto.BuildingResponse(b.id, b.name, COUNT(a.id))
			FROM Building b LEFT JOIN Apartment a ON a.building = b
			GROUP BY b.id, b.name
			ORDER BY b.id
			""")
	List<BuildingResponse> findAllWithApartmentCount();
}
