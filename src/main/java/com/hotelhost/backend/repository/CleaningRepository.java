package com.hotelhost.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelhost.backend.entity.Cleaning;

public interface CleaningRepository extends JpaRepository<Cleaning, Long> {
}
