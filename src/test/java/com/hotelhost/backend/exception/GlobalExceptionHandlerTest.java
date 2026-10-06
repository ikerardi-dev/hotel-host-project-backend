package com.hotelhost.backend.exception;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiExceptionUsesItsStatusAndCode() throws Exception {
		mockMvc.perform(get("/test/api-exception"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("BOOKING_OVERLAP"))
				.andExpect(jsonPath("$.message").value("Las fechas se solapan con otra reserva"));
	}

	@Test
	void invalidBodyReturnsValidationError() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message", startsWith("name: ")));
	}

	@Test
	void malformedJsonReturnsMalformedRequest() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ esto no es json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
	}

	@Test
	void unknownRouteReturnsNotFound() throws Exception {
		mockMvc.perform(get("/test/no-existe"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
		mockMvc.perform(delete("/test/api-exception"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void missingParameterReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/test/required-param"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void unexpectedErrorReturnsInternalErrorWithoutDetails() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("Ha ocurrido un error inesperado"));
	}

	// Controller solo para el test: provoca cada tipo de error
	@RestController
	static class TestController {

		record Payload(@NotBlank String name) {
		}

		@GetMapping("/test/api-exception")
		void apiException() {
			throw new ApiException(HttpStatus.CONFLICT, "BOOKING_OVERLAP", "Las fechas se solapan con otra reserva");
		}

		@PostMapping("/test/validation")
		void validation(@Valid @RequestBody Payload payload) {
		}

		@GetMapping("/test/required-param")
		void requiredParam(@RequestParam String date) {
		}

		@GetMapping("/test/unexpected")
		void unexpected() {
			throw new IllegalStateException("Detalle interno que no debe llegar al cliente");
		}
	}
}
