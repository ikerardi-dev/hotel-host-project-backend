package com.hotelhost.backend.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

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

import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BuildingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

// Contra la base de datos real; @Transactional deshace los datos de cada test al terminar
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerTest {

	private static final String PASSWORD = "Secreta123";
	// NIE ficticio, para no chocar con empleados reales de la base de datos
	private static final String TEST_DNI = "X0000000T";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BuildingRepository buildingRepository;

	@Autowired
	private ApartmentRepository apartmentRepository;

	@Autowired
	private CleaningRepository cleaningRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Long buildingId;

	@BeforeEach
	void setUp() {
		userRepository.save(user("admin.test@hotelhost.local", UserRole.ADMIN));
		userRepository.save(user("limpieza.test@hotelhost.local", UserRole.CLEANING));
		buildingId = buildingRepository.findAll().getFirst().getId();
	}

	@Test
	void adminCreatesEmployeeWithTemporaryPassword() throws Exception {
		createEmployee(adminToken(), "lucia.test@hotelhost.local", TEST_DNI)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("lucia.test@hotelhost.local"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.buildings[0].id").value(buildingId));

		// El nuevo empleado entra con la contraseña temporal y debe cambiarla
		login("lucia.test@hotelhost.local", "Temporal123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.role").value("CLEANING"))
				.andExpect(jsonPath("$.user.mustChangePassword").value(true));
	}

	@Test
	void duplicatedEmailIsRejected() throws Exception {
		createEmployee(adminToken(), "limpieza.test@hotelhost.local", TEST_DNI)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void invalidDniIsRejected() throws Exception {
		createEmployee(adminToken(), "lucia.test@hotelhost.local", "1234")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void cleaningStaffCannotManageEmployees() throws Exception {
		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, bearer("limpieza.test@hotelhost.local")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void listContainsOnlyCleaningStaff() throws Exception {
		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].email", hasItem("limpieza.test@hotelhost.local")))
				.andExpect(jsonPath("$[?(@.email == 'admin.test@hotelhost.local')]").isEmpty());
	}

	@Test
	void deactivatedEmployeeCannotLogInAndLosesFutureCleanings() throws Exception {
		User employee = userRepository.findByEmailIgnoreCase("limpieza.test@hotelhost.local").orElseThrow();
		cleaningRepository.save(pendingCleaning(employee, LocalDate.now().plusDays(1)));

		updateStatus(employee.getId(), false)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.employee.active").value(false))
				.andExpect(jsonPath("$.unassignedCleanings").value(1));

		login("limpieza.test@hotelhost.local", PASSWORD)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));
	}

	@Test
	void openSessionStopsWorkingWhenEmployeeIsDeactivated() throws Exception {
		String employeeToken = bearer("limpieza.test@hotelhost.local");
		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, employeeToken)).andExpect(status().isOk());

		updateStatus(employeeId(), false);

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, employeeToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void reactivatedEmployeeCanLogInAgain() throws Exception {
		Long id = userRepository.findByEmailIgnoreCase("limpieza.test@hotelhost.local").orElseThrow().getId();
		updateStatus(id, false);

		updateStatus(id, true).andExpect(jsonPath("$.employee.active").value(true));

		login("limpieza.test@hotelhost.local", PASSWORD).andExpect(status().isOk());
	}

	@Test
	void adminCannotBeDeactivatedFromEmployees() throws Exception {
		Long adminId = userRepository.findByEmailIgnoreCase("admin.test@hotelhost.local").orElseThrow().getId();

		updateStatus(adminId, false)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("EMPLOYEE_NOT_FOUND"));
	}

	@Test
	void buildingsIncludeApartmentCount() throws Exception {
		mockMvc.perform(get("/api/buildings").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Legazpi"))
				.andExpect(jsonPath("$[0].apartmentCount").value(4));
	}

	@Test
	void adminSeesEmployeeDetail() throws Exception {
		Long id = employeeId();

		mockMvc.perform(get("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("limpieza.test@hotelhost.local"));
	}

	@Test
	void adminUpdatesEmployeeData() throws Exception {
		Long id = employeeId();

		updateEmployee(id, "nuevo.email@hotelhost.local")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("nuevo.email@hotelhost.local"))
				.andExpect(jsonPath("$.phone").value("600111222"))
				.andExpect(jsonPath("$.buildings[0].id").value(buildingId));
	}

	@Test
	void updateRejectsEmailOfAnotherUser() throws Exception {
		updateEmployee(employeeId(), "admin.test@hotelhost.local")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void resetPasswordForcesChangeOnNextLogin() throws Exception {
		Long id = employeeId();

		mockMvc.perform(put("/api/employees/" + id + "/password")
				.header(HttpHeaders.AUTHORIZATION, adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"temporaryPassword\": \"Nueva1234\"}"))
				.andExpect(status().isNoContent());

		login("limpieza.test@hotelhost.local", PASSWORD).andExpect(status().isUnauthorized());
		login("limpieza.test@hotelhost.local", "Nueva1234")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.mustChangePassword").value(true));
	}

	@Test
	void activeEmployeeCannotBeDeleted() throws Exception {
		deleteEmployee(employeeId())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("EMPLOYEE_ACTIVE"));
	}

	@Test
	void deletedEmployeeKeepsNameInCleaningHistory() throws Exception {
		User employee = userRepository.findByEmailIgnoreCase("limpieza.test@hotelhost.local").orElseThrow();
		Cleaning done = pendingCleaning(employee, LocalDate.now().minusDays(1));
		done.setStatus(CleaningStatus.DONE);
		Long doneId = cleaningRepository.save(done).getId();
		updateStatus(employee.getId(), false);

		deleteEmployee(employee.getId()).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/employees/" + employee.getId()).header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/cleanings/" + doneId).header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assignedTo.id").isEmpty())
				.andExpect(jsonPath("$.assignedTo.name").value("Test Usuario"));
		// Una limpieza hecha por alguien eliminado no es una limpieza "sin asignar"
		String yesterday = LocalDate.now().minusDays(1).toString();
		mockMvc.perform(get("/api/cleanings")
				.param("from", yesterday).param("to", yesterday).param("unassigned", "true")
				.header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + doneId + ")]").isEmpty());
		login("limpieza.test@hotelhost.local", PASSWORD).andExpect(status().isUnauthorized());
	}

	private Long employeeId() {
		return userRepository.findByEmailIgnoreCase("limpieza.test@hotelhost.local").orElseThrow().getId();
	}

	private ResultActions updateEmployee(Long id, String email) throws Exception {
		String body = """
				{
				  "firstName": "Test",
				  "lastName": "Usuario Editado",
				  "dni": "%s",
				  "phone": "600111222",
				  "email": "%s",
				  "buildingIds": [%d],
				  "hireDate": "2026-10-01",
				  "notes": null
				}
				""".formatted(TEST_DNI, email, buildingId);
		return mockMvc.perform(put("/api/employees/" + id)
				.header(HttpHeaders.AUTHORIZATION, adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions deleteEmployee(Long id) throws Exception {
		return mockMvc.perform(delete("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, adminToken()));
	}

	private ResultActions createEmployee(String authorization, String email, String dni) throws Exception {
		String body = """
				{
				  "firstName": "Lucía",
				  "lastName": "Fernández Ramos",
				  "dni": "%s",
				  "phone": "654321987",
				  "email": "%s",
				  "temporaryPassword": "Temporal123",
				  "buildingIds": [%d],
				  "hireDate": "2026-10-06",
				  "notes": "Turno de mañana"
				}
				""".formatted(dni, email, buildingId);
		return mockMvc.perform(post("/api/employees")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions updateStatus(Long id, boolean active) throws Exception {
		return mockMvc.perform(patch("/api/employees/" + id + "/status")
				.header(HttpHeaders.AUTHORIZATION, adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"active\": " + active + "}"));
	}

	private String adminToken() throws Exception {
		return bearer("admin.test@hotelhost.local");
	}

	private String bearer(String email) throws Exception {
		String response = login(email, PASSWORD).andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(response, "$.token");
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
	}

	private User user(String email, UserRole role) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(PASSWORD));
		user.setFirstName("Test");
		user.setLastName("Usuario");
		user.setRole(role);
		user.setMustChangePassword(false);
		return user;
	}

	private Cleaning pendingCleaning(User assignedTo, LocalDate date) {
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(apartmentRepository.findAll().getFirst());
		cleaning.setAssignedTo(assignedTo);
		cleaning.setCleaningDate(date);
		cleaning.setStartTime(LocalTime.of(12, 0));
		cleaning.setEndTime(LocalTime.of(14, 0));
		return cleaning;
	}
}
