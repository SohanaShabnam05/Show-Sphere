package com.bookmyshow.event.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

	@Test
	void testGetSecret_afterSetSecret_shouldReturnValue() {
		// Given
		JwtProperties props = new JwtProperties();
		props.setSecret("event-jwt-secret");

		// When
		String secret = props.getSecret();

		// Then
		assertThat(secret).isEqualTo("event-jwt-secret");
		// Verify
	}
}
