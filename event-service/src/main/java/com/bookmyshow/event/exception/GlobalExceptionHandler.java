package com.bookmyshow.event.exception;

import com.bookmyshow.event.constant.EventConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(EventServiceException.class)
	public ResponseEntity<Map<String, String>> handleEventServiceException(EventServiceException exception) {

		log.warn("Event service exception: {}", exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(EventConstants.ERROR_RESPONSE_MESSAGE, exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {

		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(", "));
		log.warn("Validation failed: {}", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(EventConstants.ERROR_RESPONSE_MESSAGE, message));
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
				.body(Map.of(EventConstants.ERROR_RESPONSE_MESSAGE, "An unexpected error occurred."));
	}
}
