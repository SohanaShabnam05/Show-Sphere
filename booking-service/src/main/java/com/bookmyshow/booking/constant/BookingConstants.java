package com.bookmyshow.booking.constant;

public final class BookingConstants {

	public static final String LOG_APPLICATION_STARTUP = "Starting Booking Service application.";
	public static final String API_V1_BOOKINGS = "/api/v1/bookings";
	public static final String MY = "/my";
	public static final String CANCEL = "/cancel";
	public static final String REPORTS = "/reports";
	public static final String HAS_BOOKINGS_BY_SHOW = "/has-bookings-by-show";
	public static final String ID_PATH = "/{id}";
	public static final String X_USER_ID = "X-User-Id";
	public static final String X_USER_ROLES = "X-User-Roles";
	public static final String ROLE_ADMIN = "ROLE_ADMIN";
	public static final String AUTH_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";
	public static final String CLAIM_USER_ID = "userId";
	public static final String CLAIM_ROLES = "roles";
	public static final String ERROR_BOOKING_NOT_FOUND = "Booking not found.";
	public static final String ERROR_INSUFFICIENT_SEATS = "Insufficient seats available.";
	public static final String ERROR_COUPON_ONLY_AFTER_3_BOOKINGS = "Coupons can be applied only from 4th booking onwards.";
	public static final String ERROR_CANCEL_TOO_LATE = "Cancellation not allowed; event starts within 2 hours.";
	public static final String ERROR_SHOW_NOT_FOUND = "Show not found.";
	public static final int GST_PERCENT_MOVIE = 8;
	public static final int GST_PERCENT_CONCERT = 10;
	public static final int GST_PERCENT_LIVE_SHOW = 6;
	public static final int GST_EXEMPT_AGE_YEARS = 60;
	public static final int MIN_BOOKINGS_FOR_COUPON = 3;
	public static final int CANCEL_CUTOFF_HOURS = 2;
	public static final int REFUND_PERCENT_2_12_HRS = 10;
	public static final int REFUND_PERCENT_12_24_HRS = 50;
	public static final int REFUND_PERCENT_24_PLUS = 80;
	public static final String MY_UPCOMING = "/upcoming";
	public static final String MY_PAST = "/past";
	public static final String ERROR_RESPONSE_MESSAGE = "message";

	private BookingConstants() {
	}
}
