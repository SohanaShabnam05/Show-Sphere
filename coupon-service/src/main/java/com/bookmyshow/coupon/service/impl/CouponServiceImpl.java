package com.bookmyshow.coupon.service.impl;

import com.bookmyshow.coupon.dto.ApplyCouponRequest;
import com.bookmyshow.coupon.dto.ApplyCouponResponse;
import com.bookmyshow.coupon.dto.CreateCouponRequest;
import com.bookmyshow.coupon.entity.Coupon;
import com.bookmyshow.coupon.entity.DiscountType;
import com.bookmyshow.coupon.entity.EventCategory;
import com.bookmyshow.coupon.exception.CouponServiceException;
import com.bookmyshow.coupon.constant.CouponConstants;
import com.bookmyshow.coupon.repository.CouponRepository;
import com.bookmyshow.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

	private final CouponRepository couponRepository;

	@Override
	@Transactional
	public ApplyCouponResponse applyCoupon(ApplyCouponRequest request) {

		log.debug("Applying coupon: {}", request.getCouponCode());

		LocalDate today = LocalDate.now();
		Coupon coupon = couponRepository.findByCode(request.getCouponCode())
				.orElseThrow(() -> new CouponServiceException(CouponConstants.ERROR_COUPON_NOT_FOUND));

		if (coupon.getValidFrom().isAfter(today)) {
			throw new CouponServiceException(
					String.format(CouponConstants.ERROR_COUPON_NOT_VALID_YET, coupon.getValidFrom()));
		}
		if (coupon.getValidTo().isBefore(today)) {
			throw new CouponServiceException(
					String.format(CouponConstants.ERROR_COUPON_EXPIRED, coupon.getValidTo()));
		}
		if (coupon.getMaxRedemptions() != null && coupon.getRedemptionCount() >= coupon.getMaxRedemptions()) {
			throw new CouponServiceException(CouponConstants.ERROR_COUPON_MAX_REDEMPTIONS);
		}

		BigDecimal discountAmount;
		if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
			discountAmount = request.getAmount().multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		} else {
			discountAmount = coupon.getDiscountValue().min(request.getAmount());
		}
		BigDecimal finalAmount = request.getAmount().subtract(discountAmount).max(BigDecimal.ZERO);

		coupon.setRedemptionCount(coupon.getRedemptionCount() + 1);
		couponRepository.save(coupon);

		return ApplyCouponResponse.builder()
				.originalAmount(request.getAmount())
				.discountAmount(discountAmount)
				.finalAmount(finalAmount)
				.couponCode(coupon.getCode())
				.build();
	}

	@Override
	@Transactional
	public Coupon createCoupon(CreateCouponRequest request) {

		log.debug("Creating coupon: {}", request.getCode());
		Coupon coupon = Coupon.builder()
				.code(request.getCode())
				.discountType(request.getDiscountType())
				.discountValue(request.getDiscountValue())
				.validFrom(request.getValidFrom())
				.validTo(request.getValidTo())
				.eventCategory(request.getEventCategory())
				.maxRedemptions(request.getMaxRedemptions())
				.redemptionCount(0)
				.build();
		return couponRepository.save(coupon);
	}

	@Override
	public List<Coupon> getAllCoupons() {

		return couponRepository.findAll();
	}

	@Override
	public Coupon getCouponById(Long id) {

		return couponRepository.findById(id)
				.orElseThrow(() -> new CouponServiceException(CouponConstants.ERROR_COUPON_NOT_FOUND));
	}
}
