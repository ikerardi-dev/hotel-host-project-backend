package com.hotelhost.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelhost.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
