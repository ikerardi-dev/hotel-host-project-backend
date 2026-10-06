package com.hotelhost.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByRole(UserRole role);

	boolean existsByIdAndMustChangePasswordTrue(Long id);
}
