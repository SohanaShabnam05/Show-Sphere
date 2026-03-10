package com.bookmyshow.auth.exception;

/**
 * Thrown when email validation fails (invalid format or duplicate).
 */
public class InvalidEmailException extends RuntimeException {

	public InvalidEmailException(String message) {
		super(message);
	}
}
