package com.bookmyshow.booking.client;

import com.bookmyshow.booking.config.FeignJwtForwardConfig;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FeignJwtForwardConfigTest {

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void testAuthorizationForwardInterceptor_headerPresent_shouldAddAuthorizationToTemplate() {
		// Given
		FeignJwtForwardConfig config = new FeignJwtForwardConfig();
		var interceptor = config.authorizationForwardInterceptor();
		RequestTemplate template = new RequestTemplate();

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attrs);

		// When
		interceptor.apply(template);

		// Then
		assertThat(template.headers().get("Authorization")).contains("Bearer jwt-token");
		// Verify
		verify(request).getHeader("Authorization");
	}

	@Test
	void testAuthorizationForwardInterceptor_headerBlank_shouldNotAddAuthorization() {
		// Given
		FeignJwtForwardConfig config = new FeignJwtForwardConfig();
		var interceptor = config.authorizationForwardInterceptor();
		RequestTemplate template = new RequestTemplate();

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("   ");
		ServletRequestAttributes attrs = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attrs);

		// When
		interceptor.apply(template);

		// Then
		assertThat(template.headers().get("Authorization")).isNull();
		// Verify
	}

	@Test
	void testAuthorizationForwardInterceptor_noRequestAttributes_shouldNotFail() {
		// Given
		RequestContextHolder.resetRequestAttributes();
		FeignJwtForwardConfig config = new FeignJwtForwardConfig();
		var interceptor = config.authorizationForwardInterceptor();
		RequestTemplate template = new RequestTemplate();

		// When
		interceptor.apply(template);

		// Then
		assertThat(template.headers().get("Authorization")).isNull();
		// Verify - no NPE
	}

	@Test
	void testAuthorizationForwardInterceptor_nullRequest_shouldNotFail() {
		// Given - attrs with null request (edge case)
		ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
		when(attrs.getRequest()).thenReturn(null);
		RequestContextHolder.setRequestAttributes(attrs);

		FeignJwtForwardConfig config = new FeignJwtForwardConfig();
		var interceptor = config.authorizationForwardInterceptor();
		RequestTemplate template = new RequestTemplate();

		// When
		interceptor.apply(template);

		// Then
		assertThat(template.headers().get("Authorization")).isNull();
		// Verify
	}
}
