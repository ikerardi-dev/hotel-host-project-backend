package com.hotelhost.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hotelhost.backend.dto.ChangePasswordRequest;
import com.hotelhost.backend.dto.UpdateProfileRequest;
import com.hotelhost.backend.dto.UserResponse;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.exception.ApiException;
import com.hotelhost.backend.repository.UserRepository;

// Test unitario: sin Spring ni base de datos, los colaboradores son mocks
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static final Long USER_ID = 7L;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	@Test
	void firstAccessChangesThePasswordWithoutAskingForTheCurrentOne() {
		User user = user(true, true);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(passwordEncoder.encode("NuevaClave1")).thenReturn("hash-nuevo");

		UserResponse response = userService.changePassword(USER_ID, new ChangePasswordRequest(null, "NuevaClave1"));

		assertThat(user.getPasswordHash()).isEqualTo("hash-nuevo");
		assertThat(user.isMustChangePassword()).isFalse();
		assertThat(response.mustChangePassword()).isFalse();
		verify(passwordEncoder, never()).matches(anyString(), anyString());
	}

	@Test
	void outsideFirstAccessAWrongCurrentPasswordIsRejected() {
		User user = user(true, false);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("incorrecta", "hash-actual")).thenReturn(false);

		assertThatThrownBy(() -> userService.changePassword(USER_ID, new ChangePasswordRequest("incorrecta", "NuevaClave1")))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("INVALID_CURRENT_PASSWORD");
		assertThat(user.getPasswordHash()).isEqualTo("hash-actual");
	}

	@Test
	void outsideFirstAccessTheCurrentPasswordIsRequired() {
		User user = user(true, false);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userService.changePassword(USER_ID, new ChangePasswordRequest(null, "NuevaClave1")))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("INVALID_CURRENT_PASSWORD");
	}

	@Test
	void disabledUserLosesAccessToTheirData() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(false, false)));

		assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
				.isInstanceOf(ApiException.class)
				.extracting("code").isEqualTo("UNAUTHORIZED");
	}

	@Test
	void profileUpdateTrimsTheValuesAndClearsAnEmptyPhone() {
		User user = user(true, false);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		userService.updateProfile(USER_ID, new UpdateProfileRequest("  Carlos ", " Ruiz García ", "   "));

		assertThat(user.getFirstName()).isEqualTo("Carlos");
		assertThat(user.getLastName()).isEqualTo("Ruiz García");
		assertThat(user.getPhone()).isNull();
		assertThat(user.getEmail()).isEqualTo("maria@test.es");
	}

	private static User user(boolean active, boolean mustChangePassword) {
		User user = new User();
		user.setEmail("maria@test.es");
		user.setPasswordHash("hash-actual");
		user.setFirstName("María");
		user.setLastName("Test");
		user.setPhone("600000000");
		user.setRole(UserRole.CLEANING);
		user.setActive(active);
		user.setMustChangePassword(mustChangePassword);
		return user;
	}
}
