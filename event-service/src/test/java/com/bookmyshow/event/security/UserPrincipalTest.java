package com.bookmyshow.event.security;

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
	void testNoArgsConstructor_shouldCreateWithNulls() {
		// Given / When
		UserPrincipal principal = new UserPrincipal();

		// Then
		assertThat(principal.getUserId()).isNull();
		assertThat(principal.getEmail()).isNull();
		// Verify
	}
}
