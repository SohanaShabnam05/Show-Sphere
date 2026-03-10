package com.bookmyshow.gateway.filter;

import com.bookmyshow.gateway.config.JwtProperties;
import com.bookmyshow.gateway.constant.GatewayConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global filter that validates JWT for non-auth paths and forwards user claims to downstream services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationGatewayFilterFactory implements GlobalFilter, Ordered {

	private final JwtProperties jwtProperties;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String path = exchange.getRequest().getPath().value();

		if (path.startsWith(GatewayConstants.PATH_AUTH) || path.startsWith(GatewayConstants.PATH_ACTUATOR)) {
			return chain.filter(exchange);
		}

		String authHeader = exchange.getRequest().getHeaders().getFirst(GatewayConstants.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith(GatewayConstants.BEARER_PREFIX)) {
			return writeUnauthorized(exchange.getResponse());
		}

		String token = authHeader.substring(GatewayConstants.BEARER_PREFIX.length()).trim();
		Claims claims = parseToken(token);
		if (claims == null) {
			return writeUnauthorized(exchange.getResponse());
		}

		Object userId = claims.get(GatewayConstants.CLAIM_USER_ID);
		String email = claims.getSubject();
		@SuppressWarnings("unchecked")
		List<String> roles = claims.get(GatewayConstants.CLAIM_ROLES, List.class);
		String rolesHeader = roles != null ? String.join(",", roles) : "";

		ServerHttpRequest mutated = exchange.getRequest().mutate()
				.header(GatewayConstants.X_USER_ID, userId != null ? userId.toString() : "")
				.header(GatewayConstants.X_USER_EMAIL, email != null ? email : "")
				.header(GatewayConstants.X_USER_ROLES, rolesHeader)
				.build();

		return chain.filter(exchange.mutate().request(mutated).build());
	}

	private Claims parseToken(String token) {

		try {
			SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
			return Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (Exception e) {
			log.debug("Invalid or expired token");
			return null;
		}
	}

	private Mono<Void> writeUnauthorized(ServerHttpResponse response) {

		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		String body = "{\"message\":\"" + GatewayConstants.UNAUTHORIZED_MESSAGE + "\"}";
		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		return response.writeWith(Mono.just(buffer));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
