package com.bookmyshow.auth.exception;

/**
 * Thrown when mobile number is not exactly 10 digits.
 */
public class InvalidPhoneException extends RuntimeException {

	public InvalidPhoneException(String message) {
		super(message);
	}
}
