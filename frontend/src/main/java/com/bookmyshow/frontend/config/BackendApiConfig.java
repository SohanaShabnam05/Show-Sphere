package com.bookmyshow.frontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class BackendApiConfig {

	@Value("${backend.api.base-url:http://localhost:8888}")
	private String baseUrl;

	@Value("${jwt.cookie-name:BMS_TOKEN}")
	private String jwtCookieName;

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate rest = new RestTemplate();
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add((request, body, execution) -> {
			String token = getTokenFromRequest();
			if (token != null && !token.isBlank()) {
				request.getHeaders().set("Authorization", "Bearer " + token);
			}
			return execution.execute(request, body);
		});
		rest.setInterceptors(interceptors);
		return rest;
	}

	@Bean
	public String backendBaseUrl() {
		return baseUrl;
	}

	private String getTokenFromRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) return null;
		HttpServletRequest req = attrs.getRequest();
		if (req.getCookies() == null) return null;
		for (Cookie c : req.getCookies()) {
			if (jwtCookieName.equals(c.getName())) return c.getValue();
		}
		return null;
	}
}
