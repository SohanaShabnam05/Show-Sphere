package com.bookmyshow.event.constant;

/**
 * Holds all constants used across the Event Service.
 */
public final class EventConstants {

	public static final String LOG_APPLICATION_STARTUP = "Starting Event Service application.";
	public static final String API_V1_EVENTS = "/api/v1/events";
	public static final String ID_PATH = "/{id}";
	public static final String THEATERS = "/theaters";
	public static final String SHOWS = "/shows";
	public static final String RESERVE = "/reserve";
	public static final String RELEASE = "/release";
	public static final String DETAILS = "/details";
	public static final String REPAIR = "/repair";
	public static final String SEARCH = "/search";
	public static final String ERROR_EVENT_NOT_FOUND = "Event not found.";
	public static final String ERROR_THEATER_NOT_FOUND = "Theater not found.";
	public static final String ERROR_SHOW_NOT_FOUND = "Show not found.";
	public static final String ERROR_SHOW_OVERLAP = "Same event cannot be scheduled at the same venue at overlapping time.";
	public static final String ERROR_EVENT_HAS_BOOKINGS = "Cannot delete event with associated bookings.";
	public static final String ERROR_RELEASE_EXCEEDS_RESERVED = "Cannot release more than reserved seats. Reserved: %d, requested: %d.";
	public static final String ERROR_RESPONSE_MESSAGE = "message";
	public static final String X_USER_ROLES = "X-User-Roles";
	public static final String ROLE_ADMIN = "ROLE_ADMIN";
	public static final String BEARER_PREFIX = "Bearer ";
	public static final String CLAIM_ROLES = "roles";
	public static final String CLAIM_USER_ID = "userId";

	private EventConstants() {
	}
}
