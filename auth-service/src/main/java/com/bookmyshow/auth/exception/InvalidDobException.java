package com.bookmyshow.auth.exception;

/**
 * Thrown when date of birth is missing or invalid.
 */
public class InvalidDobException extends RuntimeException {

	public InvalidDobException(String message) {
		super(message);
	}
}
