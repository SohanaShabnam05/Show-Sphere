package com.bookmyshow.gateway.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

	@Test
	void testSetAndGetSecret_shouldReturnValue() {
		// Given
		JwtProperties props = new JwtProperties();

		// When
		props.setSecret("my-secret-key");

		// Then
		assertThat(props.getSecret()).isEqualTo("my-secret-key");

		// Verify
		props.setSecret("other");
		assertThat(props.getSecret()).isEqualTo("other");
	}
}
