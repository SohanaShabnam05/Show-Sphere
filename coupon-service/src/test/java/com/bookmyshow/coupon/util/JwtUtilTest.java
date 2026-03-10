package com.bookmyshow.coupon.util;

import com.bookmyshow.coupon.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

	@Test
	void parseToken_validSignedToken_shouldReturnClaims() {
		// Given
		String secret = "BookMyShowCouponServiceSecretKeyForJWTTokenGenerationMustBeLongEnough";
		JwtProperties props = new JwtProperties();
		props.setSecret(secret);
		JwtUtil jwtUtil = new JwtUtil(props);

		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("user@example.com")
				.claim("roles", List.of("ROLE_USER"))
				.signWith(key)
				.compact();

		// When
		var claims = jwtUtil.parseToken(token);

		// THEN
		assertThat(claims.getSubject()).isEqualTo("user@example.com");
		assertThat(claims.get("roles", List.class)).contains("ROLE_USER");
	}
}

