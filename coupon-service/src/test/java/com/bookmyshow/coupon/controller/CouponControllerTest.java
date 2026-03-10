package com.bookmyshow.coupon.controller;

import com.bookmyshow.coupon.config.JwtFilter;
import com.bookmyshow.coupon.config.SecurityConfig;
import com.bookmyshow.coupon.dto.ApplyCouponRequest;
import com.bookmyshow.coupon.dto.ApplyCouponResponse;
import com.bookmyshow.coupon.dto.CreateCouponRequest;
import com.bookmyshow.coupon.entity.Coupon;
import com.bookmyshow.coupon.entity.DiscountType;
import com.bookmyshow.coupon.service.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CouponController.class)
@Import(SecurityConfig.class)
class CouponControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CouponService couponService;

	@MockitoBean
	private JwtFilter jwtFilter;

	@BeforeEach
	void setUp() throws Exception {
		// Allow request processing to reach controller; security chain adds this filter.
		doAnswer(invocation -> {
			ServletRequest req = invocation.getArgument(0);
			ServletResponse res = invocation.getArgument(1);
			FilterChain chain = invocation.getArgument(2);
			chain.doFilter(req, res);
			return null;
		}).when(jwtFilter).doFilter(any(), any(), any());
	}

	@Test
	void applyCoupon_authenticatedUser_shouldReturn200() throws Exception {
		// Given
		ApplyCouponRequest request = ApplyCouponRequest.builder()
				.couponCode("MOVIE10")
				.amount(new BigDecimal("500.00"))
				.build();
		ApplyCouponResponse response = ApplyCouponResponse.builder()
				.couponCode("MOVIE10")
				.originalAmount(new BigDecimal("500.00"))
				.discountAmount(new BigDecimal("50.00"))
				.finalAmount(new BigDecimal("450.00"))
				.build();
		when(couponService.applyCoupon(any(ApplyCouponRequest.class))).thenReturn(response);

		// When
		var result = mockMvc.perform(post("/api/v1/coupons/apply")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(user("u").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.couponCode").value("MOVIE10"))
				.andExpect(jsonPath("$.finalAmount").value(450.00))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void applyCoupon_unauthenticated_shouldReturn401() throws Exception {
		// When
		var result = mockMvc.perform(post("/api/v1/coupons/apply")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"couponCode\":\"MOVIE10\",\"amount\":100}"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void createCoupon_adminRole_shouldReturn201() throws Exception {
		// Given
		CreateCouponRequest request = CreateCouponRequest.builder()
				.code("NEW10")
				.discountType(DiscountType.PERCENTAGE)
				.discountValue(new BigDecimal("10.00"))
				.validFrom(LocalDate.now())
				.validTo(LocalDate.now().plusDays(10))
				.build();
		Coupon created = Coupon.builder().id(1L).code("NEW10").discountType(DiscountType.PERCENTAGE).discountValue(new BigDecimal("10.00"))
				.validFrom(request.getValidFrom()).validTo(request.getValidTo()).build();
		when(couponService.createCoupon(any(CreateCouponRequest.class))).thenReturn(created);

		// When
		var result = mockMvc.perform(post("/api/v1/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.code").value("NEW10"))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(201);
	}

	@Test
	void createCoupon_userRole_shouldReturn403() throws Exception {
		// Given
		CreateCouponRequest request = CreateCouponRequest.builder()
				.code("NEW10")
				.discountType(DiscountType.PERCENTAGE)
				.discountValue(new BigDecimal("10.00"))
				.validFrom(LocalDate.now())
				.validTo(LocalDate.now().plusDays(10))
				.build();

		// When
		var result = mockMvc.perform(post("/api/v1/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(user("u").roles("USER")))
				.andExpect(status().isForbidden())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void createCoupon_invalidBody_shouldReturn400WithMessage() throws Exception {
		// When
		var result = mockMvc.perform(post("/api/v1/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"discountType\":\"PERCENTAGE\",\"discountValue\":10}")
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getContentAsString()).contains("code");
	}

	@Test
	void getAllCoupons_adminRole_shouldReturn200() throws Exception {
		// Given
		when(couponService.getAllCoupons()).thenReturn(java.util.List.of(Coupon.builder().id(1L).code("A").build()));

		// When
		var result = mockMvc.perform(get("/api/v1/coupons")
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("A"))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}
}

