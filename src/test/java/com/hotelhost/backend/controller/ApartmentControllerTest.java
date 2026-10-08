package com.hotelhost.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.hotelhost.backend.entity.Apartment;
import com.hotelhost.backend.entity.Booking;
import com.hotelhost.backend.entity.BookingChannel;
import com.hotelhost.backend.entity.Cleaning;
import com.hotelhost.backend.entity.CleaningStatus;
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

// Contra la base de datos real; @Transactional deshace los datos de cada test al terminar
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApartmentControllerTest {

	private static final String PASSWORD = "Secreta123";
	private static final String LEGAZPI_A = "$[?(@.building.name == 'Legazpi' && @.code == 'A')]";

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

	private Apartment legazpiA;

	@BeforeEach
	void setUp() {
		userRepository.save(user("admin.test@hotelhost.local", UserRole.ADMIN));
		userRepository.save(user("limpieza.test@hotelhost.local", UserRole.CLEANING));
		legazpiA = apartmentRepository.findAllWithBuildingAndBeds().stream()
				.filter(a -> a.getBuilding().getName().equals("Legazpi") && a.getCode().equals("A"))
				.findFirst()
				.orElseThrow();
	}

	@Test
	void listsAllApartmentsWithBuildingAndBeds() throws Exception {
		apartments()
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(19))
				.andExpect(jsonPath(LEGAZPI_A + ".capacity").value(2))
				.andExpect(jsonPath(LEGAZPI_A + ".beds[0].type").value("DOUBLE_BED"));
	}

	@Test
	void apartmentWithoutBookingsOrCleaningsIsFree() throws Exception {
		apartments().andExpect(jsonPath(LEGAZPI_A + ".status").value("FREE"));
	}

	@Test
	void apartmentWithGuestInsideIsOccupied() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		bookingRepository.save(booking("Laura Gómez", now.minusDays(1), now.plusDays(2)));
		bookingRepository.save(booking("Próximo Huésped", now.plusDays(3), now.plusDays(5)));

		apartments()
				.andExpect(jsonPath(LEGAZPI_A + ".status").value("OCCUPIED"))
				.andExpect(jsonPath(LEGAZPI_A + ".currentGuest.guestName").value("Laura Gómez"))
				.andExpect(jsonPath(LEGAZPI_A + ".nextGuest.guestName").value("Próximo Huésped"));
	}

	@Test
	void apartmentWithPendingCleaningTodayIsPendingCleaning() throws Exception {
		cleaningRepository.save(cleaning(CleaningStatus.PENDING));

		apartments().andExpect(jsonPath(LEGAZPI_A + ".status").value("PENDING_CLEANING"));
	}

	@Test
	void apartmentCleanedTodayIsReady() throws Exception {
		cleaningRepository.save(cleaning(CleaningStatus.DONE));

		apartments().andExpect(jsonPath(LEGAZPI_A + ".status").value("READY"));
	}

	@Test
	void findsOneApartmentById() throws Exception {
		mockMvc.perform(get("/api/apartments/" + legazpiA.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer("admin.test@hotelhost.local")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("A"))
				.andExpect(jsonPath("$.building.name").value("Legazpi"));
	}

	@Test
	void calendarReturnsBookingsTouchingThePeriodWithTheirCleaning() throws Exception {
		Booking inside = bookingRepository.save(booking("Dentro", LocalDateTime.of(2030, 5, 10, 16, 0), LocalDateTime.of(2030, 5, 14, 11, 0)));
		bookingRepository.save(booking("Fuera", LocalDateTime.of(2030, 7, 1, 16, 0), LocalDateTime.of(2030, 7, 3, 11, 0)));
		Cleaning cleaning = cleaning(CleaningStatus.PENDING);
		cleaning.setBooking(inside);
		cleaning.setCleaningDate(LocalDate.of(2030, 5, 14));
		cleaningRepository.save(cleaning);

		mockMvc.perform(get("/api/apartments/" + legazpiA.getId() + "/bookings")
				.param("from", "2030-05-01").param("to", "2030-05-31")
				.header(HttpHeaders.AUTHORIZATION, bearer("admin.test@hotelhost.local")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].guestName").value("Dentro"))
				.andExpect(jsonPath("$[0].cleaning.date").value("2030-05-14"));

		mockMvc.perform(get("/api/apartments/" + legazpiA.getId() + "/cleanings")
				.param("from", "2030-05-01").param("to", "2030-05-31")
				.header(HttpHeaders.AUTHORIZATION, bearer("admin.test@hotelhost.local")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].date").value("2030-05-14"));
	}

	@Test
	void cleaningStaffCannotSeeApartments() throws Exception {
		mockMvc.perform(get("/api/apartments").header(HttpHeaders.AUTHORIZATION, bearer("limpieza.test@hotelhost.local")))
				.andExpect(status().isForbidden());
	}

	private ResultActions apartments() throws Exception {
		return mockMvc.perform(get("/api/apartments").header(HttpHeaders.AUTHORIZATION, bearer("admin.test@hotelhost.local")));
	}

	private String bearer(String email) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(response, "$.token");
	}

	private Booking booking(String guestName, LocalDateTime checkIn, LocalDateTime checkOut) {
		Booking booking = new Booking();
		booking.setApartment(legazpiA);
		booking.setGuestName(guestName);
		booking.setChannel(BookingChannel.DIRECT);
		booking.setCheckIn(checkIn);
		booking.setCheckOut(checkOut);
		booking.setGuests(2);
		return booking;
	}

	private Cleaning cleaning(CleaningStatus status) {
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(legazpiA);
		cleaning.setCleaningDate(LocalDate.now());
		cleaning.setStartTime(LocalTime.of(12, 0));
		cleaning.setEndTime(LocalTime.of(14, 0));
		cleaning.setStatus(status);
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
