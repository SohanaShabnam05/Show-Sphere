package com.bookmyshow.auth.config;

import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtUtil jwtUtil;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private JwtAuthenticationFilter filter;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterInternal_missingAuthorizationHeader_shouldLeaveContextEmpty() throws Exception {
		// Given
		when(request.getHeader("Authorization")).thenReturn(null);

		// When
		filter.doFilterInternal(request, response, filterChain);

		// THEN
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

		// verify
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtUtil);
	}

	@Test
	void doFilterInternal_validToken_shouldSetAuthenticationWithRoles() throws Exception {
		// Given
		String token = "valid-token";
		when(request.getHeader("Authorization")).thenReturn(AuthConstants.BEARER_PREFIX + token);

		Claims claims = mock(Claims.class);
		when(jwtUtil.parseToken(token)).thenReturn(claims);
		when(claims.getSubject()).thenReturn("user@example.com");
		when(claims.get(AuthConstants.CLAIM_ROLES)).thenReturn(List.of("ROLE_USER", "ROLE_ADMIN"));

		// When
		filter.doFilterInternal(request, response, filterChain);

		// THEN
		var auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.getName()).isEqualTo("user@example.com");
		assertThat(auth.getAuthorities())
				.extracting("authority")
				.containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

		// verify
		verify(jwtUtil).parseToken(token);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilterInternal_invalidToken_shouldNotThrowAndLeaveContextEmpty() throws Exception {
		// Given
		String token = "bad-token";
		when(request.getHeader("Authorization")).thenReturn(AuthConstants.BEARER_PREFIX + token);
		when(jwtUtil.parseToken(token)).thenThrow(new JwtException("invalid"));

		// When
		filter.doFilterInternal(request, response, filterChain);

		// THEN
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

		// verify
		verify(jwtUtil).parseToken(token);
		verify(filterChain).doFilter(request, response);
	}
}

