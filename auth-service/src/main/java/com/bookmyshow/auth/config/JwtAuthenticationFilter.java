package com.bookmyshow.auth.config;

import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.util.JwtUtil;
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
 * Filter that validates JWT from Authorization header and sets Spring Security context
 * with principal (email) and authorities from the roles claim. Idempotent; invalid/missing
 * token leaves context empty so downstream rules (e.g. authenticated()) apply.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(AuthConstants.BEARER_PREFIX.length()).trim();
		if (token.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			Claims claims = jwtUtil.parseToken(token);
			if (claims == null) {
				filterChain.doFilter(request, response);
				return;
			}
			String subject = claims.getSubject();
			if (subject == null || subject.isBlank()) {
				filterChain.doFilter(request, response);
				return;
			}
			List<SimpleGrantedAuthority> authorities = getAuthoritiesFromClaims(claims);
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(subject, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtException e) {
			log.debug("Invalid or expired JWT: {}", e.getMessage());
		}
		filterChain.doFilter(request, response);
	}

	@SuppressWarnings("unchecked")
	private List<SimpleGrantedAuthority> getAuthoritiesFromClaims(Claims claims) {
		Object rolesObj = claims.get(AuthConstants.CLAIM_ROLES);
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
