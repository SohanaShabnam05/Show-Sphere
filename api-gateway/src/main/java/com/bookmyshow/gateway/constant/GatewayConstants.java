package com.bookmyshow.gateway.constant;

/**
 * Holds all constants used across the API Gateway.
 */
public final class GatewayConstants {

	public static final String LOG_APPLICATION_STARTUP = "Starting API Gateway application.";
	public static final String AUTHORIZATION = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";
	public static final String X_USER_ID = "X-User-Id";
	public static final String X_USER_EMAIL = "X-User-Email";
	public static final String X_USER_ROLES = "X-User-Roles";
	public static final String PATH_AUTH = "/api/v1/auth";
	public static final String PATH_ACTUATOR = "/actuator";
	public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
	public static final String CLAIM_SUB = "sub";
	public static final String CLAIM_USER_ID = "userId";
	public static final String CLAIM_ROLES = "roles";

	private GatewayConstants() {
	}
}
