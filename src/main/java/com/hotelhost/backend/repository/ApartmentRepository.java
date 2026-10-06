package com.hotelhost.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelhost.backend.entity.Apartment;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
}
