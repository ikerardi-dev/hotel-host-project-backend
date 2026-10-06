package com.hotelhost.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(CorsConfigTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CorsConfigTest.TestController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class CorsConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowsRequestsFromTheFrontendOrigin() throws Exception {
		mockMvc.perform(options("/api/test")
				.header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
	}

	@Test
	void rejectsRequestsFromOtherOrigins() throws Exception {
		mockMvc.perform(options("/api/test")
				.header(HttpHeaders.ORIGIN, "https://web-desconocida.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
	}

	// Controller solo para el test: una ruta /api cualquiera
	@RestController
	static class TestController {

		@GetMapping("/api/test")
		void test() {
		}
	}
}
