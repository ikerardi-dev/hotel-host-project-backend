package com.hotelhost.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

// Contra la base de datos real; @Transactional deshace los datos de cada test al terminar
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeControllerTest {

	private static final String TEMPORARY_EMAIL = "temporal.test@hotelhost.local";
	private static final String REGULAR_EMAIL = "normal.test@hotelhost.local";
	private static final String PASSWORD = "Temporal123";
	private static final String NEW_PASSWORD = "NuevaClave1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void createUsers() {
		userRepository.save(user(TEMPORARY_EMAIL, true));
		userRepository.save(user(REGULAR_EMAIL, false));
	}

	@Test
	void temporaryPasswordBlocksOtherEndpoints() throws Exception {
		mockMvc.perform(get("/api/apartments").header(HttpHeaders.AUTHORIZATION, bearer(TEMPORARY_EMAIL, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("PASSWORD_CHANGE_REQUIRED"));
	}

	@Test
	void temporaryPasswordStillAllowsReadingOwnData() throws Exception {
		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(TEMPORARY_EMAIL, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mustChangePassword").value(true));
	}

	@Test
	void firstAccessChangesPasswordWithoutCurrentOne() throws Exception {
		changePassword(bearer(TEMPORARY_EMAIL, PASSWORD), null, NEW_PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mustChangePassword").value(false));

		login(TEMPORARY_EMAIL, NEW_PASSWORD).andExpect(status().isOk());
		login(TEMPORARY_EMAIL, PASSWORD).andExpect(status().isUnauthorized());
	}

	@Test
	void weakPasswordIsRejected() throws Exception {
		changePassword(bearer(TEMPORARY_EMAIL, PASSWORD), null, "corta")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void outsideFirstAccessTheCurrentPasswordIsRequired() throws Exception {
		changePassword(bearer(REGULAR_EMAIL, PASSWORD), "incorrecta", NEW_PASSWORD)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_CURRENT_PASSWORD"));
	}

	@Test
	void outsideFirstAccessChangesPasswordWithTheCurrentOne() throws Exception {
		changePassword(bearer(REGULAR_EMAIL, PASSWORD), PASSWORD, NEW_PASSWORD).andExpect(status().isOk());

		login(REGULAR_EMAIL, NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void userUpdatesOwnProfileButNotTheEmail() throws Exception {
		mockMvc.perform(put("/api/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(REGULAR_EMAIL, PASSWORD))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\": \"Carlos\", \"lastName\": \"Ruiz García\", \"phone\": \"612890123\", \"email\": \"otro@hotelhost.local\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Carlos"))
				.andExpect(jsonPath("$.lastName").value("Ruiz García"))
				.andExpect(jsonPath("$.phone").value("612890123"))
				.andExpect(jsonPath("$.email").value(REGULAR_EMAIL));
	}

	@Test
	void profileRequiresTheFirstName() throws Exception {
		mockMvc.perform(put("/api/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(REGULAR_EMAIL, PASSWORD))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	private ResultActions changePassword(String authorization, String currentPassword, String newPassword)
			throws Exception {
		String current = currentPassword == null ? "null" : "\"" + currentPassword + "\"";
		return mockMvc.perform(put("/api/me/password")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\": " + current + ", \"newPassword\": \"" + newPassword + "\"}"));
	}

	private String bearer(String email, String password) throws Exception {
		String response = login(email, password).andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(response, "$.token");
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
	}

	private User user(String email, boolean mustChangePassword) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(PASSWORD));
		user.setFirstName("Test");
		user.setLastName("Usuario");
		user.setRole(UserRole.CLEANING);
		user.setMustChangePassword(mustChangePassword);
		return user;
	}
}
