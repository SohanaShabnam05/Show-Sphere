package com.bookmyshow.booking.exception;

import com.bookmyshow.booking.constant.BookingConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleBookingServiceException_shouldReturn400() {
		// Given
		BookingServiceException ex = new BookingServiceException("error");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleBookingServiceException(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(BookingConstants.ERROR_RESPONSE_MESSAGE, "error");
	}

	@Test
	void handleAccessDenied_shouldReturn403Json() {
		// Given
		AccessDeniedException ex = new AccessDeniedException("denied");

		// When
		ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(403);
		assertThat(response.getBody())
				.containsEntry("error", "Access Denied")
				.containsEntry("status", 403);
	}

	@Test
	void handleAuthorizationDenied_shouldReturn403Json() {
		// Given
		AuthorizationDeniedException ex = new AuthorizationDeniedException("denied");

		// When
		ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(403);
		assertThat(response.getBody().get("status")).isEqualTo(403);
	}

	@Test
	void handleGenericException_shouldReturn500WithMessage() {
		// Given
		Exception ex = new RuntimeException("Unexpected");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).containsEntry(BookingConstants.ERROR_RESPONSE_MESSAGE, "An unexpected error occurred.");
	}

}

