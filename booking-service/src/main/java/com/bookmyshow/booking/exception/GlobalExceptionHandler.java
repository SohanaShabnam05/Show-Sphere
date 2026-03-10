package com.bookmyshow.booking.exception;

import com.bookmyshow.booking.constant.BookingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(BookingServiceException.class)
	public ResponseEntity<Map<String, String>> handleBookingServiceException(BookingServiceException exception) {

		log.warn("Booking service exception: {}", exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(BookingConstants.ERROR_RESPONSE_MESSAGE, exception.getMessage()));
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
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of(BookingConstants.ERROR_RESPONSE_MESSAGE, "An unexpected error occurred."));
	}
}
