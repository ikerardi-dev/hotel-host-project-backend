package com.hotelhost.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hotelhost.backend.dto.UpdateEmployeeRequest;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.BuildingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;

// Test unitario: sin Spring ni base de datos, los colaboradores son mocks
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	private static final Long EMPLOYEE_ID = 5L;

	@Mock
	private UserRepository userRepository;

	@Mock
	private BuildingRepository buildingRepository;

	@Mock
	private CleaningRepository cleaningRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private EmployeeService employeeService;

	@Test
	void resetPasswordReplacesItAndForcesChangeOnNextLogin() {
		User employee = employee(true);
		employee.setMustChangePassword(false);
		when(userRepository.findByIdAndRole(EMPLOYEE_ID, UserRole.CLEANING)).thenReturn(Optional.of(employee));
		when(passwordEncoder.encode("Temporal99")).thenReturn("hash-temporal");

		employeeService.resetPassword(EMPLOYEE_ID, "Temporal99");

		assertThat(employee.getPasswordHash()).isEqualTo("hash-temporal");
		assertThat(employee.isMustChangePassword()).isTrue();
	}

	@Test
	void activeEmployeeIsNotDeleted() {
		when(userRepository.findByIdAndRole(EMPLOYEE_ID, UserRole.CLEANING)).thenReturn(Optional.of(employee(true)));

		assertThatThrownBy(() -> employeeService.delete(EMPLOYEE_ID))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("EMPLOYEE_ACTIVE");
		verify(userRepository, never()).deleteById(anyLong());
		verify(cleaningRepository, never()).detachFromEmployee(anyLong(), anyString());
	}

	@Test
	void deletingKeepsTheNameInTheCleaningHistory() {
		when(userRepository.findByIdAndRole(EMPLOYEE_ID, UserRole.CLEANING)).thenReturn(Optional.of(employee(false)));

		employeeService.delete(EMPLOYEE_ID);

		verify(cleaningRepository).detachFromEmployee(EMPLOYEE_ID, "Lucía Fernández");
		verify(userRepository).deleteById(EMPLOYEE_ID);
	}

	@Test
	void updateRejectsADniOfAnotherEmployee() {
		when(userRepository.findByIdAndRole(EMPLOYEE_ID, UserRole.CLEANING)).thenReturn(Optional.of(employee(true)));
		when(userRepository.existsByDniAndIdNot("12345678Z", EMPLOYEE_ID)).thenReturn(true);
		UpdateEmployeeRequest request = new UpdateEmployeeRequest("Lucía", "Fernández", "12345678z", "600000000",
				"lucia@hotelhost.local", Set.of(1L), LocalDate.of(2026, 10, 1), null);

		assertThatThrownBy(() -> employeeService.update(EMPLOYEE_ID, request))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("DNI_ALREADY_EXISTS");
	}

	private User employee(boolean active) {
		User user = new User();
		user.setFirstName("Lucía");
		user.setLastName("Fernández");
		user.setRole(UserRole.CLEANING);
		user.setActive(active);
		return user;
	}
}
