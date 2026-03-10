package com.bookmyshow.booking.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards the incoming request's Authorization header to downstream services
 * (coupon-service, event-service, auth-service) so they see the same JWT.
 */
@Configuration
public class FeignJwtForwardConfig {

	@Bean
	public RequestInterceptor authorizationForwardInterceptor() {
		return (RequestTemplate template) -> {
			ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs != null) {
				HttpServletRequest request = attrs.getRequest();
				if (request != null) {
					String auth = request.getHeader("Authorization");
					if (auth != null && !auth.isBlank()) {
						template.header("Authorization", auth);
					}
				}
			}
		};
	}
}

