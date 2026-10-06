package com.hotelhost.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelhost.backend.entity.Building;

public interface BuildingRepository extends JpaRepository<Building, Long> {
}
