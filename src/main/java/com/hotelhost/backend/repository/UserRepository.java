package com.hotelhost.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByRole(UserRole role);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByDni(String dni);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	boolean existsByDniAndIdNot(String dni, Long id);

	List<User> findByRoleOrderByFirstNameAscLastNameAsc(UserRole role);

	Optional<User> findByIdAndRole(Long id, UserRole role);
}
