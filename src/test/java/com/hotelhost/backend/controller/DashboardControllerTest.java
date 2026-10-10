package com.hotelhost.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingChannel;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

// Contra la base de datos real; @Transactional deshace los datos al terminar.
// Puede haber datos reales de hoy, así que se comparan las cifras antes y después de crear las del test.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest {

	private static final String PASSWORD = "Secreta123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApartmentRepository apartmentRepository;

	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private CleaningRepository cleaningRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String adminToken;

	@BeforeEach
	void setUp() throws Exception {
		userRepository.save(user("admin.test@hotelhost.local", UserRole.ADMIN));
		userRepository.save(user("limpieza.test@hotelhost.local", UserRole.CLEANING));
		adminToken = bearer("admin.test@hotelhost.local");
	}

	@Test
	void todayCountsCheckInsCheckOutsAndCleaningsAndOrdersTheTimeline() throws Exception {
		String before = dashboard();
		// Un apartamento libre durante toda la semana para no chocar con datos reales
		Apartment apartment = apartmentRepository.findAll().getLast();
		LocalDate today = LocalDate.now();

		Booking leaving = bookingRepository.save(booking(apartment, "Sale Hoy", today.minusDays(2).atTime(16, 0), today.atTime(11, 0)));
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(apartment);
		cleaning.setBooking(leaving);
		cleaning.setCleaningDate(today);
		cleaning.setStartTime(LocalTime.of(12, 0));
		cleaning.setEndTime(LocalTime.of(14, 0));
		cleaningRepository.save(cleaning);
		bookingRepository.save(booking(apartment, "Entra Hoy", today.atTime(16, 0), today.plusDays(2).atTime(11, 0)));

		String after = dashboard();

		assertThat(count(after, "checkIns")).isEqualTo(count(before, "checkIns") + 1);
		assertThat(count(after, "checkOuts")).isEqualTo(count(before, "checkOuts") + 1);
		assertThat(count(after, "pendingCleanings")).isEqualTo(count(before, "pendingCleanings") + 1);

		List<Map<String, Object>> ours = JsonPath.read(after, "$.timeline[?(@.apartmentId == " + apartment.getId() + ")]");
		assertThat(ours).extracting(event -> event.get("type")).containsExactly("CHECK_OUT", "CLEANING", "CHECK_IN");
		assertThat(ours.get(0).get("guestName")).isEqualTo("Sale Hoy");
		assertThat(ours.get(1).get("cleaningStatus")).isEqualTo("PENDING");
	}

	@Test
	void cleaningStaffCannotSeeTheDashboard() throws Exception {
		mockMvc.perform(get("/api/dashboard/today").header(HttpHeaders.AUTHORIZATION, bearer("limpieza.test@hotelhost.local")))
				.andExpect(status().isForbidden());
	}

	private String dashboard() throws Exception {
		return mockMvc.perform(get("/api/dashboard/today").header(HttpHeaders.AUTHORIZATION, adminToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private static int count(String json, String field) {
		return JsonPath.read(json, "$." + field);
	}

	private String bearer(String email) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(response, "$.token");
	}

	private Booking booking(Apartment apartment, String guestName, LocalDateTime checkIn, LocalDateTime checkOut) {
		Booking booking = new Booking();
		booking.setApartment(apartment);
		booking.setGuestName(guestName);
		booking.setChannel(BookingChannel.DIRECT);
		booking.setCheckIn(checkIn);
		booking.setCheckOut(checkOut);
		booking.setGuests(1);
		return booking;
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
