package com.bookmyshow.booking.config;

import com.bookmyshow.booking.constant.BookingConstants;
import com.bookmyshow.booking.security.UserPrincipal;
import com.bookmyshow.booking.util.JwtUtil;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

	private static final String SECRET = "long-secret-long-secret-long-secret-long-secret-long-secret-123456";

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
	void testDoFilterInternal_noAuthorizationHeader_shouldCallChainAndReturn() throws Exception {
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
	void testDoFilterInternal_headerNotBearer_shouldCallChainAndReturn() throws Exception {
		// Given
		when(request.getHeader("Authorization")).thenReturn("Basic abc");

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		verify(jwtUtil, never()).parseToken(any());
		// Verify
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void testDoFilterInternal_emptyTokenAfterBearer_shouldCallChainAndReturn() throws Exception {
		// Given
		when(request.getHeader("Authorization")).thenReturn(BookingConstants.BEARER_PREFIX + "   ");

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
		when(request.getHeader("Authorization")).thenReturn(BookingConstants.BEARER_PREFIX + token);
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("user@example.com");
		when(claims.get(BookingConstants.CLAIM_USER_ID, Long.class)).thenReturn(10L);
		when(claims.get(BookingConstants.CLAIM_ROLES)).thenReturn(List.of("ROLE_USER"));
		when(jwtUtil.parseToken(token)).thenReturn(claims);

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		verify(jwtUtil).parseToken(token);
		// Verify
		var auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
		UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
		assertThat(principal.getUserId()).isEqualTo(10L);
		assertThat(principal.getEmail()).isEqualTo("user@example.com");
		assertThat(auth.getAuthorities()).hasSize(1);
	}

	@Test
	void testDoFilterInternal_nullSubject_shouldCallChainAndNotSetAuth() throws Exception {
		// Given
		String token = "token";
		when(request.getHeader("Authorization")).thenReturn(BookingConstants.BEARER_PREFIX + token);
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn(null);
		when(claims.get(BookingConstants.CLAIM_USER_ID, Long.class)).thenReturn(1L);
		when(jwtUtil.parseToken(token)).thenReturn(claims);

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		// Verify - no auth set when subject blank
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void testDoFilterInternal_blankSubject_shouldCallChainAndNotSetAuth() throws Exception {
		// Given
		String token = "token";
		when(request.getHeader("Authorization")).thenReturn(BookingConstants.BEARER_PREFIX + token);
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("  ");
		when(claims.get(BookingConstants.CLAIM_USER_ID, Long.class)).thenReturn(1L);
		when(jwtUtil.parseToken(token)).thenReturn(claims);

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		// Verify
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void testDoFilterInternal_nullUserId_shouldCallChainAndNotSetAuth() throws Exception {
		// Given
		String token = "token";
		when(request.getHeader("Authorization")).thenReturn(BookingConstants.BEARER_PREFIX + token);
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("u@e.com");
		when(claims.get(BookingConstants.CLAIM_USER_ID, Long.class)).thenReturn(null);
		when(claims.get(BookingConstants.CLAIM_USER_ID, Number.class)).thenReturn(null);
		when(jwtUtil.parseToken(token)).thenReturn(claims);

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		// Verify
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void testDoFilterInternal_jwtException_shouldCallChainAndNotSetAuth() throws Exception {
		// Given
		String token = "bad-token";
		when(request.getHeader("Authorization")).thenReturn(BookingConstants.BEARER_PREFIX + token);
		when(jwtUtil.parseToken(token)).thenThrow(new io.jsonwebtoken.JwtException("invalid"));

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		// Verify
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void testDoFilterInternal_userIdAsNumber_shouldSetPrincipal() throws Exception {
		// Given - claim as Number (Integer) when Long is null
		String token = "token";
		when(request.getHeader("Authorization")).thenReturn(BookingConstants.BEARER_PREFIX + token);
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("u@e.com");
		when(claims.get(BookingConstants.CLAIM_USER_ID, Long.class)).thenReturn(null);
		when(claims.get(BookingConstants.CLAIM_USER_ID, Number.class)).thenReturn(Integer.valueOf(5));
		when(claims.get(BookingConstants.CLAIM_ROLES)).thenReturn(List.of("ROLE_USER"));
		when(jwtUtil.parseToken(token)).thenReturn(claims);

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		var auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(((UserPrincipal) auth.getPrincipal()).getUserId()).isEqualTo(5L);
	}

	@Test
	void testDoFilterInternal_rolesNotList_shouldUseEmptyAuthorities() throws Exception {
		// Given - CLAIM_ROLES as non-List
		String token = "token";
		when(request.getHeader("Authorization")).thenReturn(BookingConstants.BEARER_PREFIX + token);
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("u@e.com");
		when(claims.get(BookingConstants.CLAIM_USER_ID, Long.class)).thenReturn(1L);
		when(claims.get(BookingConstants.CLAIM_ROLES)).thenReturn("ROLE_USER");
		when(jwtUtil.parseToken(token)).thenReturn(claims);

		// When
		jwtFilter.doFilter(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(eq(request), eq(response));
		var auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.getAuthorities()).isEmpty();
	}
}
