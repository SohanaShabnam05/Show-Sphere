package com.bookmyshow.frontend.config;

import com.bookmyshow.frontend.security.JwtCookieFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtCookieFilter jwtCookieFilter;

	public SecurityConfig(JwtCookieFilter jwtCookieFilter) {
		this.jwtCookieFilter = jwtCookieFilter;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/home", "/login", "/register", "/css/**", "/webjars/**", "/static/**").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers("/bookings/cancel/**").authenticated()
				.requestMatchers("/bookings/**", "/events/**", "/browse").authenticated()
				.anyRequest().authenticated()
			)
			.formLogin(form -> form.disable())
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/home")
				.deleteCookies("BMS_TOKEN")
				.permitAll()
			)
			.addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
