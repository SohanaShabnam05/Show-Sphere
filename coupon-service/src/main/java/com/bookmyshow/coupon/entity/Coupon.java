package com.bookmyshow.coupon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DiscountType discountType;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal discountValue;

	@Column(nullable = false)
	private LocalDate validFrom;

	@Column(nullable = false)
	private LocalDate validTo;

	/** Applicable event category; null means applicable to all. */
	@Enumerated(EnumType.STRING)
	@Column(name = "event_category")
	private EventCategory eventCategory;

	/** Max number of redemptions (e.g. first N users); null means unlimited. */
	@Column(name = "max_redemptions")
	private Integer maxRedemptions;

	@Column(name = "redemption_count", nullable = false)
	private int redemptionCount;

	/** True if coupon is no longer usable: past validTo or redemption count reached max. */
	public boolean isExpired() {
		if (validTo != null && validTo.isBefore(LocalDate.now())) {
			return true;
		}
		if (maxRedemptions != null && redemptionCount >= maxRedemptions) {
			return true;
		}
		return false;
	}
}
