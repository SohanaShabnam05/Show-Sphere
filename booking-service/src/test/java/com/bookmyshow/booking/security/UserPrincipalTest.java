package com.bookmyshow.booking.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

	@Test
	void testGetUserId_afterConstruction_shouldReturnValue() {
		// Given
		UserPrincipal principal = new UserPrincipal(10L, "user@example.com");

		// When
		Long userId = principal.getUserId();

		// Then
		assertThat(userId).isEqualTo(10L);
		// Verify
		assertThat(principal.getEmail()).isEqualTo("user@example.com");
	}

	@Test
	void testGetEmail_afterConstruction_shouldReturnValue() {
		// Given
		UserPrincipal principal = new UserPrincipal(5L, "admin@example.com");

		// When
		String email = principal.getEmail();

		// Then
		assertThat(email).isEqualTo("admin@example.com");
		// Verify
	}

	@Test
	void testSetUserId_shouldUpdateValue() {
		// Given
		UserPrincipal principal = new UserPrincipal();

		// When
		principal.setUserId(99L);

		// Then
		assertThat(principal.getUserId()).isEqualTo(99L);
		// Verify
	}

	@Test
	void testSetEmail_shouldUpdateValue() {
		// Given
		UserPrincipal principal = new UserPrincipal();

		// When
		principal.setEmail("new@example.com");

		// Then
		assertThat(principal.getEmail()).isEqualTo("new@example.com");
		// Verify
	}

	@Test
	void testNoArgsConstructor_shouldCreateWithNulls() {
		// Given / When
		UserPrincipal principal = new UserPrincipal();

		// Then
		assertThat(principal.getUserId()).isNull();
		assertThat(principal.getEmail()).isNull();
		// Verify
	}
}
