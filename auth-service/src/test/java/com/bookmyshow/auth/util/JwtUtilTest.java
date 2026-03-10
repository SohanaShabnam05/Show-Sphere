package com.bookmyshow.auth.util;

import com.bookmyshow.auth.config.JwtProperties;
import com.bookmyshow.auth.entity.User;
import com.bookmyshow.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

	@Mock
	private JwtProperties jwtProperties;

	private JwtUtil jwtUtil;

	private static final String SECRET = "a".repeat(32) + "b".repeat(32);

	@BeforeEach
	void setUp() {
		when(jwtProperties.getSecret()).thenReturn(SECRET);
		when(jwtProperties.getExpirationMs()).thenReturn(3600000L);
		jwtUtil = new JwtUtil(jwtProperties);
	}

	@Test
	void testGenerateToken_validUser_returnsNonEmptyToken() {
		// Given
		User user = User.builder()
				.id(1L)
				.email("user@example.com")
				.role(Role.ROLE_USER)
				.build();

		// When
		String token = jwtUtil.generateToken(user);

		// Then
		assertThat(token).isNotBlank();
		assertThat(token.split("\\.")).hasSize(3);

		// Verify
		Claims claims = jwtUtil.parseToken(token);
		assertThat(claims.getSubject()).isEqualTo("user@example.com");
		assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
	}

	@Test
	void testParseToken_validToken_returnsClaims() {
		// Given
		User user = User.builder().id(2L).email("admin@example.com").role(Role.ROLE_ADMIN).build();
		String token = jwtUtil.generateToken(user);

		// When
		Claims claims = jwtUtil.parseToken(token);

		// Then
		assertThat(claims).isNotNull();
		assertThat(claims.getSubject()).isEqualTo("admin@example.com");
		assertThat(claims.get("userId", Long.class)).isEqualTo(2L);

		// Verify
		assertThat(jwtUtil.getEmailFromToken(token)).isEqualTo("admin@example.com");
	}

//	@Test
//	void testParseToken_invalidToken_throwsJwtException() {
//		// Given
//		String invalidToken = "invalid.jwt.token";
//
//		// When / Then
//		assertThatThrownBy(() -> jwtUtil.parseToken(invalidToken))
//				.isInstanceOf(JwtException.class);
//
//		// Verify - getEmailFromToken would also throw when parsing invalid token
//		assertThatThrownBy(() -> jwtUtil.getEmailFromToken(invalidToken))
//				.isInstanceOf(JwtException.class);
//	}

	@Test
	void testGetEmailFromToken_validToken_returnsEmail() {
		// Given
		User user = User.builder().id(3L).email("test@mail.com").role(Role.ROLE_USER).build();
		String token = jwtUtil.generateToken(user);

		// When
		String email = jwtUtil.getEmailFromToken(token);

		// Then
		assertThat(email).isEqualTo("test@mail.com");
	}
}
