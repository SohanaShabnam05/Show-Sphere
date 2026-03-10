package com.bookmyshow.user.client;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeignJwtForwardConfigTest {

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void authorizationForwardInterceptor_withAuthorizationHeader_shouldForwardHeader() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("Bearer token");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		FeignJwtForwardConfig config = new FeignJwtForwardConfig();
		var interceptor = config.authorizationForwardInterceptor();
		RequestTemplate template = new RequestTemplate();

		// When
		interceptor.apply(template);

		// THEN
		assertThat(template.headers()).containsKey("Authorization");
		assertThat(template.headers().get("Authorization")).contains("Bearer token");
	}

	@Test
	void authorizationForwardInterceptor_withoutRequestAttributes_shouldNotFailOrAddHeader() {
		// Given
		RequestContextHolder.resetRequestAttributes();
		FeignJwtForwardConfig config = new FeignJwtForwardConfig();
		var interceptor = config.authorizationForwardInterceptor();
		RequestTemplate template = new RequestTemplate();

		// When
		interceptor.apply(template);

		// THEN
		assertThat(template.headers()).doesNotContainKey("Authorization");
	}
}

