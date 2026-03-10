package com.bookmyshow.event.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration (e.g. CORS). Security is enforced at API Gateway.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {

		registry.addMapping("/api/**").allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
	}
}
