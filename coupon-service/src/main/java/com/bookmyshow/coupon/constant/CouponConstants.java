package com.bookmyshow.coupon.constant;

public final class CouponConstants {

	public static final String LOG_APPLICATION_STARTUP = "Starting Coupon Service application.";
	public static final String API_V1_COUPONS = "/api/v1/coupons";
	public static final String APPLY = "/apply";
	public static final String ID_PATH = "/{id}";
	public static final String X_USER_ROLES = "X-User-Roles";
	public static final String ROLE_ADMIN = "ROLE_ADMIN";
	public static final String BEARER_PREFIX = "Bearer ";
	public static final String CLAIM_ROLES = "roles";
	public static final String CLAIM_USER_ID = "userId";
	public static final String ERROR_COUPON_NOT_FOUND = "Coupon not found or invalid.";
	public static final String ERROR_COUPON_NOT_VALID_YET = "Coupon is not yet valid. Valid from %s.";
	public static final String ERROR_COUPON_EXPIRED = "Coupon has expired. Valid until %s.";
	public static final String ERROR_COUPON_MAX_REDEMPTIONS = "Coupon redemption limit reached.";
	public static final String ERROR_COUPON_CATEGORY_MISMATCH = "Coupon is not applicable for this event category.";
	public static final String ERROR_RESPONSE_MESSAGE = "message";

	private CouponConstants() {
	}
}
