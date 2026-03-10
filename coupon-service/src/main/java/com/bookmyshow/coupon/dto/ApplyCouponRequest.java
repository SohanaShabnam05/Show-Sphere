package com.bookmyshow.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {

	@NotBlank(message = "Coupon code is required")
	@Schema(example = "MOVIE10")
	private String couponCode;

	@NotNull(message = "Amount is required")
	@DecimalMin("0")
	@Schema(example = "500.00")
	private BigDecimal amount;

	/** Event category for applicability check (e.g. MOVIE, CONCERT, LIVE_SHOW); optional. */
	@Schema(example = "MOVIE", description = "Required when coupon is restricted to a category")
	private String eventCategory;
}
