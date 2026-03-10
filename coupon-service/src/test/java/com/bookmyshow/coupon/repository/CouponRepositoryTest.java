package com.bookmyshow.coupon.repository;

import com.bookmyshow.coupon.entity.Coupon;
import com.bookmyshow.coupon.entity.DiscountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CouponRepositoryTest {

	@Autowired
	private CouponRepository couponRepository;

	@Test
	void findByCode_existing_shouldReturnCoupon() {
		// Given
		LocalDate today = LocalDate.now();
		Coupon saved = couponRepository.save(Coupon.builder()
				.code("CODE1")
				.discountType(DiscountType.FLAT)
				.discountValue(new BigDecimal("10.00"))
				.validFrom(today.minusDays(1))
				.validTo(today.plusDays(1))
				.redemptionCount(0)
				.build());

		// When
		var found = couponRepository.findByCode("CODE1");

		// THEN
		assertThat(found).isPresent();
		assertThat(found.get().getId()).isEqualTo(saved.getId());
	}

	@Test
	void findByCodeAndValidRange_inRange_shouldReturnCoupon() {
		// Given
		LocalDate today = LocalDate.now();
		couponRepository.save(Coupon.builder()
				.code("RANGE")
				.discountType(DiscountType.PERCENTAGE)
				.discountValue(new BigDecimal("10.00"))
				.validFrom(today.minusDays(2))
				.validTo(today.plusDays(2))
				.redemptionCount(0)
				.build());

		// When
		var found = couponRepository.findByCodeAndValidFromLessThanEqualAndValidToGreaterThanEqual(
				"RANGE", today, today);

		// THEN
		assertThat(found).isPresent();
		assertThat(found.get().getCode()).isEqualTo("RANGE");
	}

	@Test
	void deleteExpiredOrConsumed_shouldDeleteOnlyExpiredOrConsumed() {
		// Given
		LocalDate today = LocalDate.now();

		Coupon expired = Coupon.builder()
				.code("EXPIRED")
				.discountType(DiscountType.FLAT)
				.discountValue(new BigDecimal("10.00"))
				.validFrom(today.minusDays(10))
				.validTo(today.minusDays(1))
				.redemptionCount(0)
				.build();

		Coupon consumed = Coupon.builder()
				.code("CONSUMED")
				.discountType(DiscountType.FLAT)
				.discountValue(new BigDecimal("10.00"))
				.validFrom(today.minusDays(1))
				.validTo(today.plusDays(10))
				.maxRedemptions(2)
				.redemptionCount(2)
				.build();

		Coupon valid = Coupon.builder()
				.code("VALID")
				.discountType(DiscountType.FLAT)
				.discountValue(new BigDecimal("10.00"))
				.validFrom(today.minusDays(1))
				.validTo(today.plusDays(10))
				.maxRedemptions(2)
				.redemptionCount(1)
				.build();

		couponRepository.save(expired);
		couponRepository.save(consumed);
		couponRepository.save(valid);

		// When
		int deleted = couponRepository.deleteExpiredOrConsumed(today);

		// THEN
		assertThat(deleted).isEqualTo(2);
		assertThat(couponRepository.findByCode("EXPIRED")).isEmpty();
		assertThat(couponRepository.findByCode("CONSUMED")).isEmpty();
		assertThat(couponRepository.findByCode("VALID")).isPresent();
	}
}

