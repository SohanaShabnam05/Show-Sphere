package com.bookmyshow.coupon.config;

import com.bookmyshow.coupon.constant.CouponConstants;
import com.bookmyshow.coupon.security.UserPrincipal;
import com.bookmyshow.coupon.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

	@Mock
	private JwtUtil jwtUtil;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private jakarta.servlet.FilterChain filterChain;

	@InjectMocks
	private JwtFilter jwtFilter;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void testDoFilterInternal_noAuthHeader_shouldCallChainAndReturn() throws Exception {
		// Given
		when(request.getHeader("Authorization")).thenReturn(null);

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		verify(jwtUtil, never()).parseToken(any());
		// Verify
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void testDoFilterInternal_validToken_shouldSetAuthenticationAndCallChain() throws Exception {
		// Given
		String token = "valid-token";
		when(request.getHeader("Authorization")).thenReturn(CouponConstants.BEARER_PREFIX + token);
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("user@example.com");
		when(claims.get(CouponConstants.CLAIM_USER_ID, Long.class)).thenReturn(10L);
		when(claims.get(CouponConstants.CLAIM_ROLES)).thenReturn(List.of("ROLE_USER"));
		when(jwtUtil.parseToken(token)).thenReturn(claims);

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		// Verify
		var auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
		assertThat(((UserPrincipal) auth.getPrincipal()).getUserId()).isEqualTo(10L);
		assertThat(((UserPrincipal) auth.getPrincipal()).getEmail()).isEqualTo("user@example.com");
	}

	@Test
	void testDoFilterInternal_jwtException_shouldCallChainAndNotSetAuth() throws Exception {
		// Given
		when(request.getHeader("Authorization")).thenReturn(CouponConstants.BEARER_PREFIX + "bad");
		when(jwtUtil.parseToken("bad")).thenThrow(new io.jsonwebtoken.JwtException("invalid"));

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		// Verify
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
