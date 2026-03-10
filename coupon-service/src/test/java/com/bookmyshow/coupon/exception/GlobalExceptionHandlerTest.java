package com.bookmyshow.coupon.exception;

import com.bookmyshow.coupon.constant.CouponConstants;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
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
	void handleCouponServiceException_shouldReturn400WithMessage() {
		// Given
		CouponServiceException ex = new CouponServiceException("bad coupon");

		// When
		var response = handler.handleCouponServiceException(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(CouponConstants.ERROR_RESPONSE_MESSAGE, "bad coupon");
	}

	@Test
	void handleDataIntegrityViolation_duplicate_shouldReturn400WithFriendlyMessage() {
		// Given
		DataIntegrityViolationException ex = new DataIntegrityViolationException(
				"oops", new RuntimeException("Duplicate entry 'CODE' for key 'coupons.UK'"));

		// When
		var response = handler.handleDataIntegrityViolation(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsEntry(CouponConstants.ERROR_RESPONSE_MESSAGE, "Coupon code already exists.");
	}

	@Test
	void handleValidation_shouldReturn400WithFieldMessages() {
		// Given
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
		bindingResult.addError(new FieldError("target", "code", "Coupon code is required"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

		// When
		var response = handler.handleValidation(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().get(CouponConstants.ERROR_RESPONSE_MESSAGE)).contains("code:");
	}

	@Test
	void handleMessageNotReadable_shouldReturn400WithMessage() {
		// Given
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"Invalid request body", new RuntimeException("bad"), new MockHttpInputMessage(new byte[0]));

		// When
		var response = handler.handleMessageNotReadable(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).containsKey(CouponConstants.ERROR_RESPONSE_MESSAGE);
	}

	@Test
	void handleAccessDenied_shouldReturn403Json() {
		// Given
		AccessDeniedException ex = new AccessDeniedException("denied");

		// When
		var response = handler.handleAccessDenied(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(403);
		assertThat(response.getBody()).containsEntry("error", "Access Denied").containsEntry("status", 403);
	}

	@Test
	void handleAuthorizationDenied_shouldReturn403Json() {
		// Given
		AuthorizationDeniedException ex = new AuthorizationDeniedException("denied");

		// When
		var response = handler.handleAccessDenied(ex);

		// THEN
		assertThat(response.getStatusCode().value()).isEqualTo(403);
		assertThat(response.getBody().get("status")).isEqualTo(403);
	}

	@Test
	void handleGenericException_shouldReturn500WithMessage() {
		// Given
		Exception ex = new RuntimeException("boom");

		// When
		var response = handler.handleGenericException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo(Map.of(CouponConstants.ERROR_RESPONSE_MESSAGE, "boom"));
	}

	@Test
	void handleGenericException_nullMessage_shouldReturn500WithDefaultMessage() {
		// Given
		Exception ex = new RuntimeException();

		// When
		var response = handler.handleGenericException(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).containsEntry(CouponConstants.ERROR_RESPONSE_MESSAGE, "An unexpected error occurred.");
	}

	@Test
	void handleDataIntegrityViolation_nonDuplicateMessage_shouldReturn400WithCauseMessage() {
		// Given
		DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint", new RuntimeException("other error"));

		// When
		var response = handler.handleDataIntegrityViolation(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().get(CouponConstants.ERROR_RESPONSE_MESSAGE)).isEqualTo("other error");
	}

	@Test
	void handleDataIntegrityViolation_mostSpecificCauseNull_shouldUseExceptionMessage() {
		// Given
		DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint violation");

		// When
		var response = handler.handleDataIntegrityViolation(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().get(CouponConstants.ERROR_RESPONSE_MESSAGE)).isEqualTo("constraint violation");
	}

	@Test
	void handleMessageNotReadable_longMessage_shouldReturnShortFriendlyMessage() {
		// Given - message length > 100
		String longMsg = "x".repeat(150);
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				longMsg, new RuntimeException("cause"), new MockHttpInputMessage(new byte[0]));

		// When
		var response = handler.handleMessageNotReadable(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().get(CouponConstants.ERROR_RESPONSE_MESSAGE))
				.contains("Invalid request body or enum value");
	}

	@Test
	void handleDataIntegrityViolation_messageNull_shouldReturnDefaultMessage() {
		// Given - exception with null message so handler uses fallback
		DataIntegrityViolationException ex = new DataIntegrityViolationException((String) null);

		// When
		var response = handler.handleDataIntegrityViolation(ex);

		// Then
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().get(CouponConstants.ERROR_RESPONSE_MESSAGE))
				.isEqualTo("Duplicate or invalid data.");
	}
}

