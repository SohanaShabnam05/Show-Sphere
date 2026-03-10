package com.bookmyshow.coupon.dto;

import com.bookmyshow.coupon.entity.DiscountType;
import com.bookmyshow.coupon.entity.EventCategory;
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
import java.time.LocalDate;

/**
 * Request DTO for creating a coupon. Do not send id or redemptionCount.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {

	@NotBlank(message = "Coupon code is required")
	@Schema(example = "MOVIE10")
	private String code;

	@NotNull(message = "Discount type is required")
	@Schema(example = "PERCENTAGE", allowableValues = { "PERCENTAGE", "FLAT" })
	private DiscountType discountType;

	@NotNull(message = "Discount value is required")
	@DecimalMin("0")
	@Schema(example = "10.00")
	private BigDecimal discountValue;

	@NotNull(message = "Valid from date is required")
	@Schema(example = "2026-06-01")
	private LocalDate validFrom;

	@NotNull(message = "Valid to date is required")
	@Schema(example = "2026-06-25")
	private LocalDate validTo;

	@Schema(example = "MOVIE", description = "Optional; MOVIE, CONCERT, or LIVE_SHOW")
	private EventCategory eventCategory;

	@Schema(example = "5", description = "Optional; max redemptions (e.g. first N users)")
	private Integer maxRedemptions;
}
