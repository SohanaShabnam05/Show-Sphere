package com.bookmyshow.gateway.filter;

import com.bookmyshow.gateway.config.JwtProperties;
import com.bookmyshow.gateway.constant.GatewayConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationGatewayFilterFactoryTest {

	private static JwtAuthenticationGatewayFilterFactory newFilterWithSecret(String secret) {
		JwtProperties props = new JwtProperties();
		props.setSecret(secret);
		return new JwtAuthenticationGatewayFilterFactory(props);
	}

	@Test
	void filter_authPath_shouldSkipJwtValidationAndCallChain() {
		// Given
		JwtAuthenticationGatewayFilterFactory filter = newFilterWithSecret("long-secret-long-secret-long-secret-long-secret-long-secret-123456");
		GatewayFilterChain chain = mock(GatewayFilterChain.class);
		when(chain.filter(any())).thenReturn(Mono.empty());

		var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
		var exchange = MockServerWebExchange.from(request);

		// When
		filter.filter(exchange, chain).block();

		// THEN
		verify(chain).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isNull();
	}

	@Test
	void filter_missingAuthorizationHeader_shouldReturn401AndNotCallChain() {
		// Given
		JwtAuthenticationGatewayFilterFactory filter = newFilterWithSecret("long-secret-long-secret-long-secret-long-secret-long-secret-123456");
		GatewayFilterChain chain = mock(GatewayFilterChain.class);

		var request = MockServerHttpRequest.get("/api/v1/events").build();
		var exchange = MockServerWebExchange.from(request);

		// When
		filter.filter(exchange, chain).block();

		// THEN
		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(exchange.getResponse().getBodyAsString().block()).contains(GatewayConstants.UNAUTHORIZED_MESSAGE);
	}

	@Test
	void filter_invalidToken_shouldReturn401AndNotCallChain() {
		// Given
		JwtAuthenticationGatewayFilterFactory filter = newFilterWithSecret("long-secret-long-secret-long-secret-long-secret-long-secret-123456");
		GatewayFilterChain chain = mock(GatewayFilterChain.class);

		var request = MockServerHttpRequest.get("/api/v1/events")
				.header(GatewayConstants.AUTHORIZATION, GatewayConstants.BEARER_PREFIX + "not-a-jwt")
				.build();
		var exchange = MockServerWebExchange.from(request);

		// When
		filter.filter(exchange, chain).block();

		// THEN
		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(exchange.getResponse().getBodyAsString().block()).contains(GatewayConstants.UNAUTHORIZED_MESSAGE);
	}

	@Test
	void filter_validToken_shouldForwardClaimsHeadersAndCallChain() {
		// Given
		String secret = "long-secret-long-secret-long-secret-long-secret-long-secret-123456";
		JwtAuthenticationGatewayFilterFactory filter = newFilterWithSecret(secret);
		GatewayFilterChain chain = mock(GatewayFilterChain.class);

		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("user@example.com")
				.claim(GatewayConstants.CLAIM_USER_ID, 7L)
				.claim(GatewayConstants.CLAIM_ROLES, List.of("ROLE_USER", "ROLE_ADMIN"))
				.signWith(key)
				.compact();

		var request = MockServerHttpRequest.get("/api/v1/events")
				.header(GatewayConstants.AUTHORIZATION, GatewayConstants.BEARER_PREFIX + token)
				.build();
		var exchange = MockServerWebExchange.from(request);

		ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
				ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
		when(chain.filter(captor.capture())).thenReturn(Mono.empty());

		// When
		filter.filter(exchange, chain).block();

		// THEN
		verify(chain).filter(any());
		var mutated = captor.getValue().getRequest().getHeaders();
		assertThat(mutated.getFirst(GatewayConstants.X_USER_ID)).isEqualTo("7");
		assertThat(mutated.getFirst(GatewayConstants.X_USER_EMAIL)).isEqualTo("user@example.com");
		assertThat(mutated.getFirst(GatewayConstants.X_USER_ROLES)).isEqualTo("ROLE_USER,ROLE_ADMIN");
	}

	@Test
	void getOrder_shouldReturnHighestPrecedence() {
		// Given
		JwtAuthenticationGatewayFilterFactory filter = newFilterWithSecret("long-secret-long-secret-long-secret-long-secret-long-secret-123456");

		// When / THEN
		assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
	}

	@Test
	void filter_actuatorPath_shouldSkipJwtValidationAndCallChain() {
		// Given
		JwtAuthenticationGatewayFilterFactory filter = newFilterWithSecret("long-secret-long-secret-long-secret-long-secret-long-secret-123456");
		GatewayFilterChain chain = mock(GatewayFilterChain.class);
		when(chain.filter(any())).thenReturn(Mono.empty());

		var request = MockServerHttpRequest.get("/actuator/health").build();
		var exchange = MockServerWebExchange.from(request);

		// When
		filter.filter(exchange, chain).block();

		// Then
		verify(chain).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isNull();
	}

	@Test
	void filter_validTokenWithNullRoles_shouldForwardEmptyRolesHeader() {
		// Given
		String secret = "long-secret-long-secret-long-secret-long-secret-long-secret-123456";
		JwtAuthenticationGatewayFilterFactory filter = newFilterWithSecret(secret);
		GatewayFilterChain chain = mock(GatewayFilterChain.class);

		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("nobody@example.com")
				.claim(GatewayConstants.CLAIM_USER_ID, 99L)
				.signWith(key)
				.compact();

		var request = MockServerHttpRequest.get("/api/v1/bookings")
				.header(GatewayConstants.AUTHORIZATION, GatewayConstants.BEARER_PREFIX + token)
				.build();
		var exchange = MockServerWebExchange.from(request);

		ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
				ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
		when(chain.filter(captor.capture())).thenReturn(Mono.empty());

		// When
		filter.filter(exchange, chain).block();

		// Then
		verify(chain).filter(any());
		var mutated = captor.getValue().getRequest().getHeaders();
		assertThat(mutated.getFirst(GatewayConstants.X_USER_ID)).isEqualTo("99");
		assertThat(mutated.getFirst(GatewayConstants.X_USER_EMAIL)).isEqualTo("nobody@example.com");
		assertThat(mutated.getFirst(GatewayConstants.X_USER_ROLES)).isEqualTo("");
	}

	@Test
	void filter_authorizationNotBearer_shouldReturn401() {
		// Given
		JwtAuthenticationGatewayFilterFactory filter = newFilterWithSecret("long-secret-long-secret-long-secret-long-secret-long-secret-123456");
		GatewayFilterChain chain = mock(GatewayFilterChain.class);

		var request = MockServerHttpRequest.get("/api/v1/events")
				.header(GatewayConstants.AUTHORIZATION, "Basic xyz")
				.build();
		var exchange = MockServerWebExchange.from(request);

		// When
		filter.filter(exchange, chain).block();

		// Then
		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}
}

