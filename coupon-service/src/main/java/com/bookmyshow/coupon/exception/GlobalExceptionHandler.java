package com.bookmyshow.coupon.exception;

import com.bookmyshow.coupon.constant.CouponConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(CouponServiceException.class)
	public ResponseEntity<Map<String, String>> handleCouponServiceException(CouponServiceException exception) {

		log.warn("Coupon service exception: {}", exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(CouponConstants.ERROR_RESPONSE_MESSAGE, exception.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {

		log.warn("Data integrity violation: {}", exception.getMessage());
		String message = exception.getMostSpecificCause() != null
				? exception.getMostSpecificCause().getMessage()
				: exception.getMessage();
		if (message != null && (message.contains("Duplicate") || message.contains("UNIQUE"))) {
			message = "Coupon code already exists.";
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(CouponConstants.ERROR_RESPONSE_MESSAGE, message != null ? message : "Duplicate or invalid data."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {

		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.collect(Collectors.joining("; "));
		log.warn("Validation failed: {}", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(CouponConstants.ERROR_RESPONSE_MESSAGE, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException exception) {

		log.warn("Invalid request body: {}", exception.getMessage());
		String msg = exception.getMessage() != null && exception.getMessage().length() > 100
				? "Invalid request body or enum value (e.g. discountType: PERCENTAGE/FLAT, eventCategory: MOVIE/CONCERT/LIVE_SHOW)."
				: exception.getMessage();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(CouponConstants.ERROR_RESPONSE_MESSAGE, msg != null ? msg : "Invalid request body."));
	}

	@ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
	public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception exception) {

		log.warn("Access denied: {}", exception.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("error", "Access Denied", "status", 403));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGenericException(Exception exception) {

		log.error("Unexpected error", exception);
		String message = exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred.";
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of(CouponConstants.ERROR_RESPONSE_MESSAGE, message));
	}
}
