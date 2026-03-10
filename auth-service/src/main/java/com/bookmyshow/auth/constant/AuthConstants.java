package com.bookmyshow.auth.constant;

/**
 * Holds all constants used across the Auth Service.
 */
public final class AuthConstants {

	public static final String APPLICATION_NAME = "auth-service";
	public static final String LOG_APPLICATION_STARTUP = "Starting Auth Service application.";
	public static final String LOG_USER_REGISTERED = "User registered successfully.";
	public static final String LOG_LOGIN_SUCCESS = "Login successful for user.";
	public static final String API_V1_AUTH = "/api/v1/auth";
	public static final String INTERNAL_USERS = "/internal/users";
	public static final String REGISTER = "/register";
	public static final String LOGIN = "/login";
	public static final String USERS = "/users";
	public static final String ROLE_USER = "ROLE_USER";
	public static final String ROLE_ADMIN = "ROLE_ADMIN";
	public static final String BEARER_PREFIX = "Bearer ";
	public static final String CLAIM_ROLES = "roles";
	public static final String CLAIM_SUBJECT = "sub";
	public static final String ERROR_EMAIL_EXISTS = "User already exists with given email.";
	public static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password.";
	public static final String ERROR_USER_NOT_FOUND = "User not found.";
	public static final String ERROR_INVALID_EMAIL = "Invalid email format.";
	public static final String ERROR_PHONE_MUST_BE_10_DIGITS = "Mobile number must be exactly 10 digits.";
	public static final String ERROR_DOB_REQUIRED = "Date of birth is required.";
	public static final String ERROR_DOB_INVALID = "Date of birth must be a valid past date.";
	public static final String ERROR_UNDER_AGE = "Only users 18 years or older are allowed to sign up.";
	public static final int MINIMUM_AGE_YEARS = 18;
	public static final int PHONE_LENGTH = 10;
	public static final String REGEX_10_DIGITS = "^[0-9]{10}$";
	public static final String SWAGGER_UI_PATH = "/swagger-ui.html";
	public static final String API_DOCS_PATH = "/v3/api-docs/**";
	public static final String SWAGGER_UI_ALL = "/swagger-ui/**";
	public static final String ACTUATOR_HEALTH = "/actuator/health";
	public static final String ACTUATOR_INFO = "/actuator/info";
	public static final String ERROR_RESPONSE_MESSAGE = "message";
	public static final String ADMIN_SEED_EMAIL = "admin@system.com";
	public static final String ADMIN_SEED_PASSWORD = "Admin@123";

	private AuthConstants() {
	}
}
