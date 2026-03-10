package com.bookmyshow.gateway.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayConstantsTest {

	@Test
	void testConstants_haveExpectedValues() {
		// Given / When / Then
		assertThat(GatewayConstants.AUTHORIZATION).isEqualTo("Authorization");
		assertThat(GatewayConstants.BEARER_PREFIX).isEqualTo("Bearer ");
		assertThat(GatewayConstants.X_USER_ID).isEqualTo("X-User-Id");
		assertThat(GatewayConstants.X_USER_EMAIL).isEqualTo("X-User-Email");
		assertThat(GatewayConstants.X_USER_ROLES).isEqualTo("X-User-Roles");
		assertThat(GatewayConstants.PATH_AUTH).isEqualTo("/api/v1/auth");
		assertThat(GatewayConstants.PATH_ACTUATOR).isEqualTo("/actuator");
		assertThat(GatewayConstants.UNAUTHORIZED_MESSAGE).isEqualTo("Unauthorized");
		assertThat(GatewayConstants.CLAIM_USER_ID).isEqualTo("userId");
		assertThat(GatewayConstants.CLAIM_ROLES).isEqualTo("roles");
		assertThat(GatewayConstants.LOG_APPLICATION_STARTUP).contains("API Gateway");
	}
}
