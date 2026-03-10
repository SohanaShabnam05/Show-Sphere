package com.bookmyshow.auth.util;

import com.bookmyshow.auth.config.JwtProperties;
import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Utility for generating and parsing JWT tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

	private final JwtProperties jwtProperties;

	/**
	 * Generates a JWT token for the given user.
	 *
	 * @param user the authenticated user
	 * @return signed JWT string
	 */
	public String generateToken(User user) {

		List<String> roles = List.of(user.getRole().name());

		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());
		SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
				.subject(user.getEmail())
				.claim(AuthConstants.CLAIM_ROLES, roles)
				.claim("userId", user.getId())
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	/**
	 * Parses and validates the JWT token, returning claims.
	 *
	 * @param token the JWT string
	 * @return claims or null if invalid
	 */
	public Claims parseToken(String token) {

		SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * Extracts email (subject) from token.
	 *
	 * @param token the JWT string
	 * @return email or null
	 */
	public String getEmailFromToken(String token) {

		Claims claims = parseToken(token);
		return claims == null ? null : claims.getSubject();
	}
}
