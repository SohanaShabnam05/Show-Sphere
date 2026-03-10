package com.bookmyshow.auth.exception;

import com.bookmyshow.auth.constant.AuthConstants;
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

/**
 * Global exception handler for Auth Service. Returns consistent error response format.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthServiceException.class)
	public ResponseEntity<Map<String, String>> handleAuthServiceException(AuthServiceException exception) {

		log.warn("Auth service exception: {}", exception.getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(AuthConstants.ERROR_RESPONSE_MESSAGE, exception.getMessage()));
	}

	@ExceptionHandler(InvalidEmailException.class)
	public ResponseEntity<Map<String, String>> handleInvalidEmailException(InvalidEmailException exception) {

		log.warn("Invalid email: {}", exception.getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(AuthConstants.ERROR_RESPONSE_MESSAGE, exception.getMessage()));
	}

	@ExceptionHandler(InvalidPhoneException.class)
	public ResponseEntity<Map<String, String>> handleInvalidPhoneException(InvalidPhoneException exception) {

		log.warn("Invalid phone: {}", exception.getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(AuthConstants.ERROR_RESPONSE_MESSAGE, exception.getMessage()));
	}

	@ExceptionHandler(InvalidDobException.class)
	public ResponseEntity<Map<String, String>> handleInvalidDobException(InvalidDobException exception) {

		log.warn("Invalid DOB: {}", exception.getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(AuthConstants.ERROR_RESPONSE_MESSAGE, exception.getMessage()));
	}

	@ExceptionHandler(UnderAgeException.class)
	public ResponseEntity<Map<String, String>> handleUnderAgeException(UnderAgeException exception) {

		log.warn("Under age registration attempt: {}", exception.getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(AuthConstants.ERROR_RESPONSE_MESSAGE, exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException exception) {

		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(", "));
		log.warn("Validation failed: {}", message);

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(AuthConstants.ERROR_RESPONSE_MESSAGE, message));
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

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of(AuthConstants.ERROR_RESPONSE_MESSAGE, "An unexpected error occurred."));
	}
}
