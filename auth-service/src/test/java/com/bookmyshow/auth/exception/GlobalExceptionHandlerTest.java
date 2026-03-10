package com.bookmyshow.auth.exception;

import com.bookmyshow.auth.constant.AuthConstants;
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
	void handleAuthServiceException_shouldReturn400WithMessage() {
		// Given
		AuthServiceException ex = new AuthServiceException("error");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleAuthServiceException(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(AuthConstants.ERROR_RESPONSE_MESSAGE, "error");
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
	void handleValidationException_shouldReturn400WithCombinedMessages() {
		// Given
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
		bindingResult.addError(new FieldError("target", "field1", "first error"));
		bindingResult.addError(new FieldError("target", "field2", "second error"));

		MethodArgumentNotValidException ex =
				new MethodArgumentNotValidException(null, bindingResult);

		// When
		ResponseEntity<Map<String, String>> response = handler.handleValidationException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().get(AuthConstants.ERROR_RESPONSE_MESSAGE))
				.contains("first error")
				.contains("second error");
	}

	@Test
	void handleInvalidEmailException_shouldReturn400WithMessage() {
		// Given
		InvalidEmailException ex = new InvalidEmailException("User already exists with given email.");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleInvalidEmailException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(AuthConstants.ERROR_RESPONSE_MESSAGE, ex.getMessage());
	}

	@Test
	void handleInvalidPhoneException_shouldReturn400WithMessage() {
		// Given
		InvalidPhoneException ex = new InvalidPhoneException("Mobile number must be exactly 10 digits.");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleInvalidPhoneException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(AuthConstants.ERROR_RESPONSE_MESSAGE, ex.getMessage());
	}

	@Test
	void handleInvalidDobException_shouldReturn400WithMessage() {
		// Given
		InvalidDobException ex = new InvalidDobException("Date of birth is required.");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleInvalidDobException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(AuthConstants.ERROR_RESPONSE_MESSAGE, ex.getMessage());
	}

	@Test
	void handleUnderAgeException_shouldReturn400WithMessage() {
		// Given
		UnderAgeException ex = new UnderAgeException("Only users 18 years or older are allowed to sign up.");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleUnderAgeException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(AuthConstants.ERROR_RESPONSE_MESSAGE, ex.getMessage());
	}

	@Test
	void handleGenericException_shouldReturn500WithGenericMessage() {
		// Given
		Exception ex = new RuntimeException("Unexpected failure");

		// When
		ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).containsEntry(AuthConstants.ERROR_RESPONSE_MESSAGE, "An unexpected error occurred.");
	}
}

