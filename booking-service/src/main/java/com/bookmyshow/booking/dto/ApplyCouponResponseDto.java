package com.bookmyshow.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Response from coupon-service apply API (Feign).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponResponseDto {

	private BigDecimal originalAmount;
	private BigDecimal discountAmount;
	private BigDecimal finalAmount;
	private String couponCode;
}
