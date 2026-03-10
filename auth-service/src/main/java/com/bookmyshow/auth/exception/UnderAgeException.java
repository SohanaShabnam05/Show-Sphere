package com.bookmyshow.auth.exception;

/**
 * Thrown when user is under 18 years of age at registration.
 */
public class UnderAgeException extends RuntimeException {

	public UnderAgeException(String message) {
		super(message);
	}
}
