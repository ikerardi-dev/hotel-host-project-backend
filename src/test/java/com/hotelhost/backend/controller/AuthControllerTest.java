package com.hotelhost.backend.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AuthControllerTest {

	private static final String PASSWORD = "Secreta123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void createUsers() {
		userRepository.save(cleaningUser("maria.test@hotelhost.local", true));
		userRepository.save(cleaningUser("baja.test@hotelhost.local", false));
	}

	@Test
	void loginWithValidCredentialsReturnsTokenAndUser() throws Exception {
		login("maria.test@hotelhost.local", PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", notNullValue()))
				.andExpect(jsonPath("$.expiresAt", notNullValue()))
				.andExpect(jsonPath("$.user.email").value("maria.test@hotelhost.local"))
				.andExpect(jsonPath("$.user.role").value("CLEANING"))
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist());
	}

	@Test
	void loginIgnoresEmailCase() throws Exception {
		login("MARIA.Test@hotelhost.local", PASSWORD).andExpect(status().isOk());
	}

	@Test
	void wrongPasswordReturnsInvalidCredentials() throws Exception {
		login("maria.test@hotelhost.local", "otra")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
	}

	@Test
	void unknownEmailReturnsTheSameError() throws Exception {
		login("nadie@hotelhost.local", PASSWORD)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
	}

	@Test
	void disabledUserCannotLogIn() throws Exception {
		login("baja.test@hotelhost.local", PASSWORD)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));
	}

	@Test
	void invalidBodyReturnsValidationError() throws Exception {
		login("no-es-un-email", "")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void meWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void meWithInvalidTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-falso"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void meWithTokenReturnsTheLoggedUser() throws Exception {
		String response = login("maria.test@hotelhost.local", PASSWORD).andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(response, "$.token");

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("maria.test@hotelhost.local"))
				.andExpect(jsonPath("$.firstName").value("María"));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
	}

	private User cleaningUser(String email, boolean active) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(PASSWORD));
		user.setFirstName("María");
		user.setLastName("Test");
		user.setRole(UserRole.CLEANING);
		user.setActive(active);
		return user;
	}
}
