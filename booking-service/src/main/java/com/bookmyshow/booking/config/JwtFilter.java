package com.bookmyshow.booking.config;

import com.bookmyshow.booking.constant.BookingConstants;
import com.bookmyshow.booking.security.UserPrincipal;
import com.bookmyshow.booking.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates JWT from Authorization header, extracts sub (email), roles, and userId,
 * sets UserPrincipal and authorities in SecurityContext. No dependency on client-provided
 * identity headers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith(BookingConstants.BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(BookingConstants.BEARER_PREFIX.length()).trim();
		if (token.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			Claims claims = jwtUtil.parseToken(token);
			String email = claims.getSubject();
			if (email == null || email.isBlank()) {
				filterChain.doFilter(request, response);
				return;
			}

			Long userId = claims.get(BookingConstants.CLAIM_USER_ID, Long.class);
			if (userId == null) {
				Number n = claims.get(BookingConstants.CLAIM_USER_ID, Number.class);
				userId = n != null ? n.longValue() : null;
			}
			if (userId == null) {
				filterChain.doFilter(request, response);
				return;
			}

			List<SimpleGrantedAuthority> authorities = getAuthoritiesFromClaims(claims);
			UserPrincipal principal = new UserPrincipal(userId, email);
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(principal, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtException e) {
			log.debug("Invalid or expired JWT: {}", e.getMessage());
		}
		filterChain.doFilter(request, response);
	}

	@SuppressWarnings("unchecked")
	private List<SimpleGrantedAuthority> getAuthoritiesFromClaims(Claims claims) {
		Object rolesObj = claims.get(BookingConstants.CLAIM_ROLES);
		if (rolesObj == null) {
			return Collections.emptyList();
		}
		if (rolesObj instanceof List<?> list) {
			return list.stream()
					.filter(String.class::isInstance)
					.map(String.class::cast)
					.filter(s -> !s.isBlank())
					.map(SimpleGrantedAuthority::new)
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
}
