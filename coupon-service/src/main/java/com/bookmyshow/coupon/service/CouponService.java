package com.bookmyshow.coupon.service;

import com.bookmyshow.coupon.dto.ApplyCouponRequest;
import com.bookmyshow.coupon.dto.ApplyCouponResponse;
import com.bookmyshow.coupon.dto.CreateCouponRequest;
import com.bookmyshow.coupon.entity.Coupon;

import java.util.List;

/**
 * Service contract for coupon operations: apply, create, list.
 */
public interface CouponService {

	ApplyCouponResponse applyCoupon(ApplyCouponRequest request);

	Coupon createCoupon(CreateCouponRequest request);

	List<Coupon> getAllCoupons();

	Coupon getCouponById(Long id);
}
