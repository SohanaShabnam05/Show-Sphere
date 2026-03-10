package com.bookmyshow.booking.client;

import com.bookmyshow.booking.dto.ApplyCouponRequestDto;
import com.bookmyshow.booking.dto.ApplyCouponResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Coupon Service (apply coupon at booking).
 */
@FeignClient(name = "coupon-service", configuration = com.bookmyshow.booking.config.FeignJwtForwardConfig.class)
public interface CouponServiceClient {

	@PostMapping("/api/v1/coupons/apply")
	ApplyCouponResponseDto applyCoupon(@RequestBody ApplyCouponRequestDto request);
}
