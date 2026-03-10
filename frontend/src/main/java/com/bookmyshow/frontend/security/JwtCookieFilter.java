package com.bookmyshow.frontend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtCookieFilter extends OncePerRequestFilter {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.cookie-name:BMS_TOKEN}")
	private String jwtCookieName;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String token = null;
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if (jwtCookieName.equals(c.getName())) {
					token = c.getValue();
					break;
				}
			}
		}
		if (token != null && !token.isBlank()) {
			try {
				SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
				Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
				String email = claims.getSubject();
				Long userId = claims.get("userId", Long.class);
				@SuppressWarnings("unchecked")
				List<String> roles = claims.get("roles", List.class);
				if (roles == null) roles = Collections.emptyList();
				List<SimpleGrantedAuthority> authorities = roles.stream()
						.map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
						.collect(Collectors.toList());
				UsernamePasswordAuthenticationToken auth =
						new UsernamePasswordAuthenticationToken(email, null, authorities);
				auth.setDetails(userId);
				SecurityContextHolder.getContext().setAuthentication(auth);
			} catch (Exception e) {
				log.debug("Invalid or expired JWT in cookie", e);
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}
}
