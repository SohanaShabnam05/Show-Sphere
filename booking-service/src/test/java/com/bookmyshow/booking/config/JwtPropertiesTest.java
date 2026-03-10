package com.bookmyshow.booking.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

	@Test
	void testGetSecret_afterSetSecret_shouldReturnValue() {
		// Given
		JwtProperties props = new JwtProperties();
		props.setSecret("my-secret-key");

		// When
		String secret = props.getSecret();

		// Then
		assertThat(secret).isEqualTo("my-secret-key");
		// Verify
		assertThat(props.getSecret()).isNotNull();
	}

	@Test
	void testSetSecret_shouldStoreValue() {
		// Given
		JwtProperties props = new JwtProperties();

		// When
		props.setSecret("another-secret");

		// Then
		assertThat(props.getSecret()).isEqualTo("another-secret");
		// Verify
	}
}
