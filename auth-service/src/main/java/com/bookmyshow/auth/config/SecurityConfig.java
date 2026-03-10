package com.bookmyshow.auth.config;

import com.bookmyshow.auth.constant.AuthConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(AuthConstants.API_V1_AUTH + AuthConstants.REGISTER,
								AuthConstants.API_V1_AUTH + AuthConstants.LOGIN).permitAll()
						.requestMatchers(AuthConstants.API_V1_AUTH + "/**").authenticated()
						.requestMatchers(AuthConstants.INTERNAL_USERS + "/**").permitAll()
						.requestMatchers(AuthConstants.SWAGGER_UI_PATH).permitAll()
						.requestMatchers(AuthConstants.SWAGGER_UI_ALL).permitAll()
						.requestMatchers(AuthConstants.API_DOCS_PATH).permitAll()
						.requestMatchers(AuthConstants.ACTUATOR_HEALTH, AuthConstants.ACTUATOR_INFO).permitAll()
						.anyRequest().authenticated());

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
