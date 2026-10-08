package com.hotelhost.backend.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.hotelhost.backend.entity.CleaningChecklistItem;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

// Contra la base de datos real; @Transactional deshace los datos de cada test al terminar
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CleaningControllerTest {

	private static final String PASSWORD = "Secreta123";
	private static final String MARIA = "maria.test@hotelhost.local";
	private static final String JAVIER = "javier.test@hotelhost.local";
	private static final String ADMIN = "admin.test@hotelhost.local";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApartmentRepository apartmentRepository;

	@Autowired
	private CleaningRepository cleaningRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private User maria;
	private Cleaning todayCleaning;

	@BeforeEach
	void setUp() {
		userRepository.save(user(ADMIN, UserRole.ADMIN));
		maria = userRepository.save(user(MARIA, UserRole.CLEANING));
		userRepository.save(user(JAVIER, UserRole.CLEANING));
		todayCleaning = cleaningRepository.save(cleaning(maria, LocalDate.now(), CleaningStatus.PENDING));
	}

	@Test
	void employeeSeesHerCleaningsWithChecklist() throws Exception {
		mockMvc.perform(get("/api/me/cleanings").header(HttpHeaders.AUTHORIZATION, bearer(MARIA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(todayCleaning.getId()))
				.andExpect(jsonPath("$[0].status").value("PENDING"))
				.andExpect(jsonPath("$[0].apartment.buildingName", notNullValue()))
				.andExpect(jsonPath("$[0].checklist.length()").value(6));
	}

	@Test
	void overdueUnfinishedCleaningsAreIncludedButOldFinishedOnesAreNot() throws Exception {
		Cleaning overdue = cleaningRepository.save(cleaning(maria, LocalDate.now().minusDays(1), CleaningStatus.PENDING));
		Cleaning oldDone = cleaningRepository.save(cleaning(maria, LocalDate.now().minusDays(1), CleaningStatus.DONE));

		mockMvc.perform(get("/api/me/cleanings").header(HttpHeaders.AUTHORIZATION, bearer(MARIA)))
				.andExpect(jsonPath("$[*].id", hasItem(overdue.getId().intValue())))
				.andExpect(jsonPath("$[*].id", not(hasItem(oldDone.getId().intValue()))));
	}

	@Test
	void anotherEmployeeCannotSeeOrTouchTheCleaning() throws Exception {
		mockMvc.perform(get("/api/cleanings/" + todayCleaning.getId()).header(HttpHeaders.AUTHORIZATION, bearer(JAVIER)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("CLEANING_NOT_ASSIGNED"));
		updateStatus(JAVIER, "IN_PROGRESS").andExpect(status().isForbidden());
	}

	@Test
	void adminCanSeeAnyCleaning() throws Exception {
		mockMvc.perform(get("/api/cleanings/" + todayCleaning.getId()).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assignedTo.id").value(maria.getId()));
	}

	@Test
	void cleaningGoesFromPendingToInProgressToDone() throws Exception {
		updateStatus(MARIA, "IN_PROGRESS")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.startedAt", notNullValue()));

		updateStatus(MARIA, "DONE")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DONE"))
				.andExpect(jsonPath("$.finishedAt", notNullValue()));
	}

	@Test
	void cleaningCannotSkipSteps() throws Exception {
		updateStatus(MARIA, "DONE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
	}

	@Test
	void checklistCanOnlyBeMarkedOnceStarted() throws Exception {
		Long itemId = todayCleaning.getChecklistItems().getFirst().getId();

		markItem(itemId, true)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CLEANING_NOT_IN_PROGRESS"));

		updateStatus(MARIA, "IN_PROGRESS");
		markItem(itemId, true)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checklist[0].done").value(true));
	}

	@Test
	void employeeCanWriteAnIncident() throws Exception {
		mockMvc.perform(patch("/api/cleanings/" + todayCleaning.getId() + "/incident")
				.header(HttpHeaders.AUTHORIZATION, bearer(MARIA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"incidentNotes\": \"Mando del aire sin pilas\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.incidentNotes").value("Mando del aire sin pilas"));
	}

	private ResultActions updateStatus(String email, String newStatus) throws Exception {
		return mockMvc.perform(patch("/api/cleanings/" + todayCleaning.getId() + "/status")
				.header(HttpHeaders.AUTHORIZATION, bearer(email))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"" + newStatus + "\"}"));
	}

	private ResultActions markItem(Long itemId, boolean done) throws Exception {
		return mockMvc.perform(patch("/api/cleanings/" + todayCleaning.getId() + "/checklist/" + itemId)
				.header(HttpHeaders.AUTHORIZATION, bearer(MARIA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"done\": " + done + "}"));
	}

	private String bearer(String email) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(response, "$.token");
	}

	private Cleaning cleaning(User assignedTo, LocalDate date, CleaningStatus status) {
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(apartmentRepository.findAll().getFirst());
		cleaning.setAssignedTo(assignedTo);
		cleaning.setCleaningDate(date);
		cleaning.setStartTime(LocalTime.of(12, 0));
		cleaning.setEndTime(LocalTime.of(14, 0));
		cleaning.setStatus(status);
		for (int i = 1; i <= 6; i++) {
			cleaning.addChecklistItem(new CleaningChecklistItem("Tarea " + i, i));
		}
		return cleaning;
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
}
