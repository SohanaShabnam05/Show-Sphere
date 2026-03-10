package com.bookmyshow.coupon.service.impl;

import com.bookmyshow.coupon.constant.CouponConstants;
import com.bookmyshow.coupon.dto.ApplyCouponRequest;
import com.bookmyshow.coupon.dto.ApplyCouponResponse;
import com.bookmyshow.coupon.dto.CreateCouponRequest;
import com.bookmyshow.coupon.entity.Coupon;
import com.bookmyshow.coupon.entity.DiscountType;
import com.bookmyshow.coupon.exception.CouponServiceException;
import com.bookmyshow.coupon.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

	@Mock
	private CouponRepository couponRepository;

	@InjectMocks
	private CouponServiceImpl couponService;

	@Test
	void applyCoupon_percentageDiscount_shouldReturnDiscountAndFinalAmount() {
		// Given
		LocalDate today = LocalDate.now();
		Coupon coupon = Coupon.builder()
				.code("MOVIE10")
				.discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.valueOf(10))
				.validFrom(today.minusDays(1))
				.validTo(today.plusDays(1))
				.build();
		when(couponRepository.findByCode("MOVIE10")).thenReturn(Optional.of(coupon));
		when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("MOVIE10")
				.amount(new BigDecimal("333.33"))
				.build();

		// When
		ApplyCouponResponse response = couponService.applyCoupon(request);

		// THEN
		assertThat(response.getCouponCode()).isEqualTo("MOVIE10");
		assertThat(response.getOriginalAmount()).isEqualByComparingTo("333.33");
		assertThat(response.getDiscountAmount()).isEqualByComparingTo("33.33");
		assertThat(response.getFinalAmount()).isEqualByComparingTo("300.00");
	}

	@Test
	void applyCoupon_flatDiscountGreaterThanAmount_shouldClampDiscountToAmountAndFinalZero() {
		// Given
		LocalDate today = LocalDate.now();
		Coupon coupon = Coupon.builder()
				.code("FLAT500")
				.discountType(DiscountType.FLAT)
				.discountValue(new BigDecimal("500.00"))
				.validFrom(today.minusDays(1))
				.validTo(today.plusDays(1))
				.build();
		when(couponRepository.findByCode("FLAT500")).thenReturn(Optional.of(coupon));
		when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("FLAT500")
				.amount(new BigDecimal("120.00"))
				.build();

		// When
		ApplyCouponResponse response = couponService.applyCoupon(request);

		// THEN
		assertThat(response.getDiscountAmount()).isEqualByComparingTo("120.00");
		assertThat(response.getFinalAmount()).isEqualByComparingTo("0.00");
	}

	@Test
	void applyCoupon_couponNotFound_shouldThrowCouponServiceException() {
		// Given
		when(couponRepository.findByCode("MISSING")).thenReturn(Optional.empty());
		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("MISSING")
				.amount(new BigDecimal("100.00"))
				.build();

		// When
		Throwable thrown = catchThrowable(() -> couponService.applyCoupon(request));

		// THEN
		assertThat(thrown)
				.isInstanceOf(CouponServiceException.class)
				.hasMessage(CouponConstants.ERROR_COUPON_NOT_FOUND);
	}

	@Test
	void applyCoupon_notYetValid_shouldThrowCouponServiceException() {
		// Given
		LocalDate today = LocalDate.now();
		LocalDate validFrom = today.plusDays(2);
		Coupon coupon = Coupon.builder()
				.code("FUTURE")
				.discountType(DiscountType.FLAT)
				.discountValue(BigDecimal.TEN)
				.validFrom(validFrom)
				.validTo(today.plusDays(10))
				.build();
		when(couponRepository.findByCode("FUTURE")).thenReturn(Optional.of(coupon));

		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("FUTURE")
				.amount(new BigDecimal("100.00"))
				.build();

		// When
		Throwable thrown = catchThrowable(() -> couponService.applyCoupon(request));

		// THEN
		assertThat(thrown)
				.isInstanceOf(CouponServiceException.class)
				.hasMessage(String.format(CouponConstants.ERROR_COUPON_NOT_VALID_YET, validFrom));
	}

	@Test
	void applyCoupon_expired_shouldThrowCouponServiceException() {
		// Given
		LocalDate today = LocalDate.now();
		LocalDate validTo = today.minusDays(1);
		Coupon coupon = Coupon.builder()
				.code("EXPIRED")
				.discountType(DiscountType.FLAT)
				.discountValue(BigDecimal.TEN)
				.validFrom(today.minusDays(10))
				.validTo(validTo)
				.build();
		when(couponRepository.findByCode("EXPIRED")).thenReturn(Optional.of(coupon));

		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("EXPIRED")
				.amount(new BigDecimal("100.00"))
				.build();

		// When
		Throwable thrown = catchThrowable(() -> couponService.applyCoupon(request));

		// THEN
		assertThat(thrown)
				.isInstanceOf(CouponServiceException.class)
				.hasMessage(String.format(CouponConstants.ERROR_COUPON_EXPIRED, validTo));
	}

	@Test
	void createCoupon_validRequest_shouldPersistWithRedemptionCountZero() {
		// Given
		LocalDate today = LocalDate.now();
		CreateCouponRequest request = CreateCouponRequest.builder()
				.code("NEW10")
				.discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.TEN)
				.validFrom(today)
				.validTo(today.plusDays(10))
				.maxRedemptions(5)
				.build();
		Coupon saved = Coupon.builder().id(99L).code("NEW10").build();
		when(couponRepository.save(any(Coupon.class))).thenReturn(saved);

		ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);

		// When
		Coupon result = couponService.createCoupon(request);

		// THEN
		assertThat(result.getId()).isEqualTo(99L);

		verify(couponRepository).save(captor.capture());
		Coupon toSave = captor.getValue();
		assertThat(toSave.getCode()).isEqualTo("NEW10");
		assertThat(toSave.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
		assertThat(toSave.getDiscountValue()).isEqualByComparingTo("10");
		assertThat(toSave.getValidFrom()).isEqualTo(today);
		assertThat(toSave.getValidTo()).isEqualTo(today.plusDays(10));
		assertThat(toSave.getMaxRedemptions()).isEqualTo(5);
		assertThat(toSave.getRedemptionCount()).isZero();
	}

	@Test
	void getAllCoupons_shouldReturnRepositoryResults() {
		// Given
		Coupon c1 = Coupon.builder().id(1L).code("A").discountType(DiscountType.FLAT).discountValue(BigDecimal.ONE)
				.validFrom(LocalDate.now().minusDays(1)).validTo(LocalDate.now().plusDays(1)).build();
		when(couponRepository.findAll()).thenReturn(List.of(c1));

		// When
		List<Coupon> coupons = couponService.getAllCoupons();

		// THEN
		assertThat(coupons).containsExactly(c1);
	}

	@Test
	void applyCoupon_flatDiscountLessThanAmount_shouldApplyFullFlatDiscount() {
		// Given
		LocalDate today = LocalDate.now();
		Coupon coupon = Coupon.builder()
				.code("FLAT50")
				.discountType(DiscountType.FLAT)
				.discountValue(new BigDecimal("50.00"))
				.validFrom(today.minusDays(1))
				.validTo(today.plusDays(1))
				.build();
		when(couponRepository.findByCode("FLAT50")).thenReturn(Optional.of(coupon));
		when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));
		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("FLAT50")
				.amount(new BigDecimal("200.00"))
				.build();

		// When
		ApplyCouponResponse response = couponService.applyCoupon(request);

		// Then
		assertThat(response.getDiscountAmount()).isEqualByComparingTo("50.00");
		assertThat(response.getFinalAmount()).isEqualByComparingTo("150.00");
		// Verify
		assertThat(response.getCouponCode()).isEqualTo("FLAT50");
	}

	@Test
	void applyCoupon_validFromEqualsToday_shouldApplySuccessfully() {
		// Given - validFrom is today (not after today)
		LocalDate today = LocalDate.now();
		Coupon coupon = Coupon.builder()
				.code("TODAY")
				.discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.valueOf(5))
				.validFrom(today)
				.validTo(today.plusDays(5))
				.build();
		when(couponRepository.findByCode("TODAY")).thenReturn(Optional.of(coupon));
		when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));
		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("TODAY")
				.amount(new BigDecimal("100.00"))
				.build();

		// When
		ApplyCouponResponse response = couponService.applyCoupon(request);

		// Then
		assertThat(response.getFinalAmount()).isEqualByComparingTo("95.00");
		assertThat(response.getDiscountAmount()).isEqualByComparingTo("5.00");
		// Verify
	}

	@Test
	void applyCoupon_validToEqualsToday_shouldApplySuccessfully() {
		// Given - validTo is today (not before today)
		LocalDate today = LocalDate.now();
		Coupon coupon = Coupon.builder()
				.code("LASTDAY")
				.discountType(DiscountType.FLAT)
				.discountValue(BigDecimal.TEN)
				.validFrom(today.minusDays(5))
				.validTo(today)
				.build();
		when(couponRepository.findByCode("LASTDAY")).thenReturn(Optional.of(coupon));
		when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));
		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("LASTDAY")
				.amount(new BigDecimal("50.00"))
				.build();

		// When
		ApplyCouponResponse response = couponService.applyCoupon(request);

		// Then
		assertThat(response.getFinalAmount()).isEqualByComparingTo("40.00");
		assertThat(response.getDiscountAmount()).isEqualByComparingTo("10.00");
		// Verify
	}

	@Test
	void applyCoupon_maxRedemptionsReached_shouldThrowCouponServiceException() {
		// Given - coupon has maxRedemptions=2 and redemptionCount=2 (already at limit)
		LocalDate today = LocalDate.now();
		Coupon coupon = Coupon.builder()
				.code("LIMIT2")
				.discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.TEN)
				.validFrom(today.minusDays(1))
				.validTo(today.plusDays(1))
				.maxRedemptions(2)
				.redemptionCount(2)
				.build();
		when(couponRepository.findByCode("LIMIT2")).thenReturn(Optional.of(coupon));

		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("LIMIT2")
				.amount(new BigDecimal("100.00"))
				.build();

		// When
		Throwable thrown = catchThrowable(() -> couponService.applyCoupon(request));

		// Then
		assertThat(thrown)
				.isInstanceOf(CouponServiceException.class)
				.hasMessage(CouponConstants.ERROR_COUPON_MAX_REDEMPTIONS);
		verify(couponRepository, never()).save(any(Coupon.class));
	}

	@Test
	void applyCoupon_success_shouldIncrementRedemptionCount() {
		// Given
		LocalDate today = LocalDate.now();
		Coupon coupon = Coupon.builder()
				.code("INC1")
				.discountType(DiscountType.FLAT)
				.discountValue(BigDecimal.ONE)
				.validFrom(today.minusDays(1))
				.validTo(today.plusDays(1))
				.maxRedemptions(5)
				.redemptionCount(0)
				.build();
		when(couponRepository.findByCode("INC1")).thenReturn(Optional.of(coupon));
		when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("INC1")
				.amount(new BigDecimal("100.00"))
				.build();

		ArgumentCaptor<Coupon> saveCaptor = ArgumentCaptor.forClass(Coupon.class);

		// When
		couponService.applyCoupon(request);

		// Then
		verify(couponRepository).save(saveCaptor.capture());
		assertThat(saveCaptor.getValue().getRedemptionCount()).isEqualTo(1);
	}
}

