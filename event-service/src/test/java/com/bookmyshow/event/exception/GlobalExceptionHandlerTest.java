package com.bookmyshow.event.exception;

import com.bookmyshow.event.constant.EventConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleEventServiceException_shouldReturn400WithMessage() {
		// Given
		EventServiceException ex = new EventServiceException("bad");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleEventServiceException(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(EventConstants.ERROR_RESPONSE_MESSAGE, "bad");
	}

	@Test
	void handleAccessDenied_shouldReturn403Json() {
		// Given
		AccessDeniedException ex = new AccessDeniedException("denied");

		// When
		ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(403);
		assertThat(response.getBody()).containsEntry("error", "Access Denied").containsEntry("status", 403);
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
	void handleValidation_shouldReturn400WithCombinedMessages() {
		// Given
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
		bindingResult.addError(new FieldError("target", "name", "Event name is required"));
		bindingResult.addError(new FieldError("target", "basePrice", "Base price is required"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

		// When
		ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().get(EventConstants.ERROR_RESPONSE_MESSAGE))
				.contains("Event name is required")
				.contains("Base price is required");
	}

	@Test
	void handleGenericException_shouldReturn500WithGenericMessage() {
		// Given
		Exception ex = new RuntimeException("boom");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).containsEntry(EventConstants.ERROR_RESPONSE_MESSAGE, "An unexpected error occurred.");
	}
}

