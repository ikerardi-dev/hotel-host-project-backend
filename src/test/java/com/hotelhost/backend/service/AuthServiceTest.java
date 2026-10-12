package com.hotelhost.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hotelhost.backend.dto.LoginRequest;
import com.hotelhost.backend.dto.LoginResponse;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.UserRepository;
import com.hotelhost.backend.security.JwtService;
import com.hotelhost.backend.security.JwtService.IssuedToken;

// Test unitario: sin Spring ni base de datos, los colaboradores son mocks
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private AuthService authService;

	@Test
	void unknownEmailIsRejectedAsInvalidCredentials() {
		when(userRepository.findByEmailIgnoreCase("nadie@test.es")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@test.es", "Secreta123")))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("INVALID_CREDENTIALS");
	}

	@Test
	void wrongPasswordGivesTheSameErrorAsUnknownEmail() {
		User user = user(true);
		when(userRepository.findByEmailIgnoreCase("maria@test.es")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("otra", "hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("maria@test.es", "otra")))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("INVALID_CREDENTIALS");
		verify(jwtService, never()).generateToken(any());
	}

	@Test
	void disabledAccountIsRejectedOnlyAfterTheRightPassword() {
		User user = user(false);
		when(userRepository.findByEmailIgnoreCase("maria@test.es")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secreta123", "hash")).thenReturn(true);

		assertThatThrownBy(() -> authService.login(new LoginRequest("maria@test.es", "Secreta123")))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("ACCOUNT_DISABLED");
	}

	@Test
	void validCredentialsReturnTheTokenAndTheUser() {
		User user = user(true);
		Instant expiresAt = Instant.parse("2030-01-01T12:00:00Z");
		when(userRepository.findByEmailIgnoreCase("maria@test.es")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secreta123", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn(new IssuedToken("token-de-prueba", expiresAt));

		// El email se limpia de espacios antes de buscarlo
		LoginResponse response = authService.login(new LoginRequest("  maria@test.es ", "Secreta123"));

		assertThat(response.token()).isEqualTo("token-de-prueba");
		assertThat(response.expiresAt()).isEqualTo(expiresAt);
		assertThat(response.user().email()).isEqualTo("maria@test.es");
		assertThat(response.user().role()).isEqualTo(UserRole.CLEANING);
	}

	private static User user(boolean active) {
		User user = new User();
		user.setEmail("maria@test.es");
		user.setPasswordHash("hash");
		user.setFirstName("María");
		user.setLastName("Test");
		user.setRole(UserRole.CLEANING);
		user.setActive(active);
		return user;
	}
}
