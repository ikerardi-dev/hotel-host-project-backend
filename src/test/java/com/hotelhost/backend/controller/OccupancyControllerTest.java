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
import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.repository.ApartmentRepository;
import com.hotelhost.backend.repository.BookingRepository;
import com.hotelhost.backend.repository.CleaningRepository;
import com.hotelhost.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

// Contra la base de datos real; @Transactional deshace los datos al terminar. Fechas de 2030.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OccupancyControllerTest {

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

	private Apartment legazpiB;
	private Apartment casaBaratura2B;

	@BeforeEach
	void setUp() {
		userRepository.save(user("admin.test@hotelhost.local", UserRole.ADMIN));
		userRepository.save(user("limpieza.test@hotelhost.local", UserRole.CLEANING));
		legazpiB = apartment("Legazpi", "B");
		casaBaratura2B = apartment("Casa Baratura", "2B");
	}

	@Test
	void returnsTheApartmentsBookingsAndCleaningsOfTheBuilding() throws Exception {
		Booking booking = bookingRepository.save(booking(legazpiB, "Gómez", LocalDateTime.of(2030, 5, 6, 16, 0), LocalDateTime.of(2030, 5, 9, 11, 0)));
		bookingRepository.save(booking(casaBaratura2B, "Otro Edificio", LocalDateTime.of(2030, 5, 6, 16, 0), LocalDateTime.of(2030, 5, 9, 11, 0)));
		Cleaning cleaning = new Cleaning();
		cleaning.setApartment(legazpiB);
		cleaning.setBooking(booking);
		cleaning.setCleaningDate(LocalDate.of(2030, 5, 9));
		cleaning.setStartTime(LocalTime.of(12, 0));
		cleaning.setEndTime(LocalTime.of(14, 0));
		cleaningRepository.save(cleaning);

		occupancy(legazpiB.getBuilding().getId(), bearer("admin.test@hotelhost.local"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.apartments.length()").value(4))
				.andExpect(jsonPath("$.apartments[0].code").value("A"))
				.andExpect(jsonPath("$.bookings.length()").value(1))
				.andExpect(jsonPath("$.bookings[0].guestName").value("Gómez"))
				.andExpect(jsonPath("$.cleanings.length()").value(1))
				.andExpect(jsonPath("$.cleanings[0].date").value("2030-05-09"));
	}

	@Test
	void cleaningStaffCannotSeeTheOccupancy() throws Exception {
		occupancy(legazpiB.getBuilding().getId(), bearer("limpieza.test@hotelhost.local"))
				.andExpect(status().isForbidden());
	}

	private ResultActions occupancy(Long buildingId, String authorization) throws Exception {
		return mockMvc.perform(get("/api/occupancy")
				.param("buildingId", buildingId.toString())
				.param("from", "2030-05-06")
				.param("to", "2030-05-12")
				.header(HttpHeaders.AUTHORIZATION, authorization));
	}

	private Apartment apartment(String building, String code) {
		return apartmentRepository.findAllWithBuildingAndBeds().stream()
				.filter(a -> a.getBuilding().getName().equals(building) && a.getCode().equals(code))
				.findFirst()
				.orElseThrow();
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
		booking.setChannel(BookingChannel.BOOKING);
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
