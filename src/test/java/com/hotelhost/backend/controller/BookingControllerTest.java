package com.hotelhost.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

// Contra la base de datos real; @Transactional deshace los datos de cada test al terminar.
// Se usan fechas de 2030 para no depender del día en que se ejecuten.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingControllerTest {

	private static final String PASSWORD = "Secreta123";
	private static final String NO_CLEANING = "null";

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

	private Long legazpiB; // 4 plazas
	private User cleaner;
	private String adminToken;

	@BeforeEach
	void setUp() throws Exception {
		userRepository.save(user("admin.test@hotelhost.local", UserRole.ADMIN, true));
		cleaner = userRepository.save(user("limpieza.test@hotelhost.local", UserRole.CLEANING, true));
		userRepository.save(user("baja.test@hotelhost.local", UserRole.CLEANING, false));
		legazpiB = apartmentRepository.findAllWithBuildingAndBeds().stream()
				.filter(a -> a.getBuilding().getName().equals("Legazpi") && a.getCode().equals("B"))
				.map(Apartment::getId)
				.findFirst()
				.orElseThrow();
		adminToken = bearer("admin.test@hotelhost.local");
	}

	@Test
	void createsBookingWithItsCleaningOnCheckOutDay() throws Exception {
		String cleaningPlan = "{\"startTime\": \"12:00\", \"endTime\": \"14:00\", \"cleanerId\": " + cleaner.getId() + "}";

		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 3, cleaningPlan)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.apartmentCode").value("B"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.cleaning.date").value("2030-05-14"))
				.andExpect(jsonPath("$.cleaning.status").value("PENDING"))
				.andExpect(jsonPath("$.cleaning.assignedToId").value(cleaner.getId()));

		var cleaning = cleaningRepository.findAll().stream()
				.filter(c -> c.getApartment().getId().equals(legazpiB))
				.findFirst()
				.orElseThrow();
		assertThat(cleaning.getChecklistItems()).hasSize(6);
	}

	@Test
	void cleaningCanBeLeftUnassigned() throws Exception {
		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, "{\"startTime\": \"12:00\", \"endTime\": \"14:00\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cleaning.assignedToId").isEmpty());
	}

	@Test
	void bookingWithoutCleaningIsAllowed() throws Exception {
		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cleaning").isEmpty());
	}

	@Test
	void overlappingBookingIsRejected() throws Exception {
		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING);

		createBooking("2030-05-13T16:00:00", "2030-05-16T11:00:00", 2, NO_CLEANING)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("BOOKING_OVERLAP"));
	}

	@Test
	void checkInOnTheSameDayAsPreviousCheckOutIsAllowed() throws Exception {
		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING);

		createBooking("2030-05-14T16:00:00", "2030-05-16T11:00:00", 2, NO_CLEANING).andExpect(status().isCreated());
	}

	@Test
	void moreGuestsThanCapacityIsRejected() throws Exception {
		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 5, NO_CLEANING)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CAPACITY_EXCEEDED"));
	}

	@Test
	void checkOutBeforeCheckInIsRejected() throws Exception {
		createBooking("2030-05-14T16:00:00", "2030-05-10T11:00:00", 2, NO_CLEANING)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_DATES"));
	}

	@Test
	void inactiveCleanerCannotBeAssigned() throws Exception {
		Long inactiveId = userRepository.findByEmailIgnoreCase("baja.test@hotelhost.local").orElseThrow().getId();
		String plan = "{\"startTime\": \"12:00\", \"endTime\": \"14:00\", \"cleanerId\": " + inactiveId + "}";

		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, plan)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_CLEANER"));
	}

	@Test
	void cleaningMustEndAfterItStarts() throws Exception {
		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, "{\"startTime\": \"14:00\", \"endTime\": \"12:00\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void availabilityListsApartmentsWithOverlappingBookings() throws Exception {
		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING);

		availability("2030-05-12T16:00:00", "2030-05-15T11:00:00")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unavailableApartmentIds", contains(legazpiB.intValue())));
		availability("2030-05-14T16:00:00", "2030-05-15T11:00:00")
				.andExpect(jsonPath("$.unavailableApartmentIds", empty()));
	}

	@Test
	void editingMovesThePendingCleaningToTheNewCheckOut() throws Exception {
		Long id = createdId(createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2,
				"{\"startTime\": \"12:00\", \"endTime\": \"14:00\"}"));

		updateBooking(id, "2030-05-10T16:00:00", "2030-05-16T11:00:00", 3)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.guests").value(3))
				.andExpect(jsonPath("$.checkOut").value("2030-05-16T11:00:00"))
				.andExpect(jsonPath("$.cleaning.date").value("2030-05-16"));
	}

	@Test
	void editingDoesNotOverlapWithItselfButDoesWithOthers() throws Exception {
		Long id = createdId(createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING));
		createBooking("2030-05-20T16:00:00", "2030-05-22T11:00:00", 2, NO_CLEANING);

		updateBooking(id, "2030-05-11T16:00:00", "2030-05-15T11:00:00", 2).andExpect(status().isOk());
		updateBooking(id, "2030-05-11T16:00:00", "2030-05-21T11:00:00", 2)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("BOOKING_OVERLAP"));
	}

	@Test
	void editingRespectsCapacity() throws Exception {
		Long id = createdId(createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING));

		updateBooking(id, "2030-05-10T16:00:00", "2030-05-14T11:00:00", 5)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CAPACITY_EXCEEDED"));
	}

	@Test
	void cancellingAlsoCancelsThePendingCleaningAndFreesTheDates() throws Exception {
		Long id = createdId(createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2,
				"{\"startTime\": \"12:00\", \"endTime\": \"14:00\"}"));

		mockMvc.perform(patch("/api/bookings/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.cleaning").isEmpty());

		var cleaning = cleaningRepository.findByBookingId(id).orElseThrow();
		assertThat(cleaning.getStatus()).isEqualTo(CleaningStatus.CANCELLED);

		// Las fechas vuelven a estar libres
		createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING).andExpect(status().isCreated());
	}

	@Test
	void cancelledBookingCannotBeEdited() throws Exception {
		Long id = createdId(createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING));
		mockMvc.perform(patch("/api/bookings/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, adminToken));

		updateBooking(id, "2030-05-10T16:00:00", "2030-05-14T11:00:00", 2)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("BOOKING_CANCELLED"));
	}

	@Test
	void availabilityCanExcludeTheBookingBeingEdited() throws Exception {
		Long id = createdId(createBooking("2030-05-10T16:00:00", "2030-05-14T11:00:00", 2, NO_CLEANING));

		mockMvc.perform(get("/api/apartments/availability")
				.param("checkIn", "2030-05-11T16:00:00").param("checkOut", "2030-05-13T11:00:00")
				.param("excludeBookingId", id.toString())
				.header(HttpHeaders.AUTHORIZATION, adminToken))
				.andExpect(jsonPath("$.unavailableApartmentIds", empty()));
	}

	@Test
	void cleaningStaffCannotCreateBookings() throws Exception {
		mockMvc.perform(post("/api/bookings")
				.header(HttpHeaders.AUTHORIZATION, bearer("limpieza.test@hotelhost.local"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
	}

	private ResultActions createBooking(String checkIn, String checkOut, int guests, String cleaningPlan) throws Exception {
		String body = """
				{
				  "apartmentId": %d,
				  "channel": "BOOKING",
				  "guestName": "Laura Gómez",
				  "guestPhone": "654987321",
				  "guestEmail": "laura@example.com",
				  "reference": "BK-123",
				  "checkIn": "%s",
				  "checkOut": "%s",
				  "guests": %d,
				  "notes": "Llegada tarde",
				  "cleaning": %s
				}
				""".formatted(legazpiB, checkIn, checkOut, guests, cleaningPlan);
		return mockMvc.perform(post("/api/bookings")
				.header(HttpHeaders.AUTHORIZATION, adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions updateBooking(Long id, String checkIn, String checkOut, int guests) throws Exception {
		String body = """
				{
				  "channel": "AIRBNB",
				  "guestName": "Laura Gómez",
				  "checkIn": "%s",
				  "checkOut": "%s",
				  "guests": %d
				}
				""".formatted(checkIn, checkOut, guests);
		return mockMvc.perform(put("/api/bookings/" + id)
				.header(HttpHeaders.AUTHORIZATION, adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private Long createdId(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

	private ResultActions availability(String checkIn, String checkOut) throws Exception {
		return mockMvc.perform(get("/api/apartments/availability")
				.param("checkIn", checkIn)
				.param("checkOut", checkOut)
				.header(HttpHeaders.AUTHORIZATION, adminToken));
	}

	private String bearer(String email) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(response, "$.token");
	}

	private User user(String email, UserRole role, boolean active) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(PASSWORD));
		user.setFirstName("Test");
		user.setLastName("Usuario");
		user.setRole(role);
		user.setActive(active);
		user.setMustChangePassword(false);
		return user;
	}
}
