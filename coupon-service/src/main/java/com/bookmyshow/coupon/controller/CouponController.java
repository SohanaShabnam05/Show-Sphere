package com.bookmyshow.coupon.controller;

import com.bookmyshow.coupon.constant.CouponConstants;
import com.bookmyshow.coupon.dto.ApplyCouponRequest;
import com.bookmyshow.coupon.dto.ApplyCouponResponse;
import com.bookmyshow.coupon.dto.CreateCouponRequest;
import com.bookmyshow.coupon.entity.Coupon;
import com.bookmyshow.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(CouponConstants.API_V1_COUPONS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Coupons", description = "Coupon and discount APIs")
public class CouponController {

	private final CouponService couponService;

	@PostMapping(CouponConstants.APPLY)
	@Operation(summary = "Apply coupon and get discounted amount")
	public ResponseEntity<ApplyCouponResponse> applyCoupon(@Valid @RequestBody ApplyCouponRequest request) {

		ApplyCouponResponse response = couponService.applyCoupon(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Create coupon (Admin only). Example: 10% off for Movies, first 5 users, expires 25 June; or 25% off for Concerts, first 3 users, expires 26 June.")
	public ResponseEntity<Coupon> createCoupon(@Valid @RequestBody CreateCouponRequest request) {

		Coupon created = couponService.createCoupon(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "List all coupons (Admin only)")
	public ResponseEntity<List<Coupon>> getAllCoupons() {

		List<Coupon> coupons = couponService.getAllCoupons();
		return ResponseEntity.ok(coupons);
	}

	@GetMapping(CouponConstants.ID_PATH)
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Get coupon by id (Admin only)")
	public ResponseEntity<Coupon> getCouponById(@PathVariable Long id) {

		Coupon coupon = couponService.getCouponById(id);
		return ResponseEntity.ok(coupon);
	}
}
