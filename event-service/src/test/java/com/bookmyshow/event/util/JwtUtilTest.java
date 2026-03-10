package com.bookmyshow.event.util;

import com.bookmyshow.event.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

	private static final String SECRET = "long-secret-long-secret-long-secret-long-secret-long-secret-123456";

	@Mock
	private JwtProperties jwtProperties;

	@InjectMocks
	private JwtUtil jwtUtil;

	@Test
	void testParseToken_validToken_shouldReturnClaims() {
		// Given
		when(jwtProperties.getSecret()).thenReturn(SECRET);
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("user@example.com")
				.claim("userId", 5L)
				.claim("roles", List.of("ROLE_USER"))
				.signWith(key)
				.compact();

		// When
		Claims claims = jwtUtil.parseToken(token);

		// Then
		assertThat(claims).isNotNull();
		assertThat(claims.getSubject()).isEqualTo("user@example.com");
		// Verify
		assertThat(claims.get("userId", Long.class)).isEqualTo(5L);
	}

	@Test
	void testParseToken_invalidToken_shouldThrow() {
		// Given
		when(jwtProperties.getSecret()).thenReturn(SECRET);

		// When / Then
		assertThatThrownBy(() -> jwtUtil.parseToken("not-a-valid-jwt"))
				.isInstanceOf(Exception.class);
		// Verify
	}
}
