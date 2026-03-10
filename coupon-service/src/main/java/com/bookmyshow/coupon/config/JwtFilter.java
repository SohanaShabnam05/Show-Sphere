package com.bookmyshow.coupon.config;

import com.bookmyshow.coupon.constant.CouponConstants;
import com.bookmyshow.coupon.security.UserPrincipal;
import com.bookmyshow.coupon.util.JwtUtil;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith(CouponConstants.BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}
		String token = authHeader.substring(CouponConstants.BEARER_PREFIX.length()).trim();
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
			Long userId = claims.get(CouponConstants.CLAIM_USER_ID, Long.class);
			if (userId == null) {
				Number n = claims.get(CouponConstants.CLAIM_USER_ID, Number.class);
				userId = n != null ? n.longValue() : null;
			}
			UserPrincipal principal = new UserPrincipal(userId, email);
			List<SimpleGrantedAuthority> authorities = getAuthoritiesFromClaims(claims);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtException e) {
			log.debug("Invalid or expired JWT: {}", e.getMessage());
		}
		filterChain.doFilter(request, response);
	}

	@SuppressWarnings("unchecked")
	private List<SimpleGrantedAuthority> getAuthoritiesFromClaims(Claims claims) {
		Object rolesObj = claims.get(CouponConstants.CLAIM_ROLES);
		if (rolesObj == null) return Collections.emptyList();
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
