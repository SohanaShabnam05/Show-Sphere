package com.bookmyshow.event.util;

import com.bookmyshow.event.config.JwtProperties;
import com.bookmyshow.event.constant.EventConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtUtil {

	private final JwtProperties jwtProperties;

	public Claims parseToken(String token) {
		SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
