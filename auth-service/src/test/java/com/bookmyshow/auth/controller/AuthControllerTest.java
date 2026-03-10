package com.bookmyshow.auth.controller;

import com.bookmyshow.auth.dto.JwtResponse;
import com.bookmyshow.auth.dto.LoginRequest;
import com.bookmyshow.auth.dto.RegisterRequest;
import com.bookmyshow.auth.entity.Role;
import com.bookmyshow.auth.entity.User;
import com.bookmyshow.auth.config.SecurityConfig;
import com.bookmyshow.auth.repository.UserRepository;
import com.bookmyshow.auth.service.AuthService;
import com.bookmyshow.auth.config.JwtAuthenticationFilter;
import com.bookmyshow.auth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private JwtUtil jwtUtil;

	@BeforeEach
	void setUp() throws Exception {
		// Let requests reach controllers; security chain adds this filter.
		doAnswer(invocation -> {
			ServletRequest req = invocation.getArgument(0);
			ServletResponse res = invocation.getArgument(1);
			FilterChain chain = invocation.getArgument(2);
			chain.doFilter(req, res);
			return null;
		}).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
	}

	@Test
	void register_validRequest_shouldReturn201AndJwtResponse() throws Exception {
		// Given
		RegisterRequest request = RegisterRequest.builder()
				.name("User")
				.email("user@example.com")
				.password("password")
				.mobile("1234567890")
				.address("addr")
				.dateOfBirth(LocalDate.now().minusYears(20))
				.build();
		JwtResponse jwtResponse = JwtResponse.builder()
				.token("jwt")
				.email("user@example.com")
				.build();
		when(authService.register(any(RegisterRequest.class))).thenReturn(jwtResponse);

		String body = objectMapper.writeValueAsString(request);

		// When
		var result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").value("jwt"))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(201);
	}

	@Test
	void login_validRequest_shouldReturn200AndJwtResponse() throws Exception {
		// Given
		LoginRequest request = new LoginRequest("user@example.com", "pwd");
		JwtResponse jwtResponse = JwtResponse.builder()
				.token("jwt")
				.email("user@example.com")
				.build();
		when(authService.login(any(LoginRequest.class))).thenReturn(jwtResponse);

		String body = objectMapper.writeValueAsString(request);

		// When
		var result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt"))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void getAllUsers_adminRole_shouldReturn200WithList() throws Exception {
		// Given
		User user = User.builder()
				.id(1L)
				.name("Admin")
				.email("admin@example.com")
				.mobile("1234567890")
				.address("addr")
				.dateOfBirth(LocalDate.now().minusYears(30))
				.role(Role.ROLE_ADMIN)
				.build();
		when(userRepository.findAll()).thenReturn(List.of(user));

		// When
		var result = mockMvc.perform(get("/api/v1/auth/users").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("admin@example.com"))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void getAllUsers_userRole_shouldReturn200() throws Exception {
		// Given
		when(userRepository.findAll()).thenReturn(List.of());

		// When
		var result = mockMvc.perform(get("/api/v1/auth/users").with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andReturn();

		// Then
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
		verify(userRepository).findAll();
	}

	@Test
	void register_missingRequiredField_shouldReturn400() throws Exception {
		// Given - invalid request (e.g. missing email)
		String body = "{\"name\":\"User\",\"password\":\"pwd\",\"mobile\":\"1234567890\",\"dateOfBirth\":\"2000-01-01\"}";

		// When
		var result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andReturn();

		// Then
		assertThat(result.getResponse().getStatus()).isEqualTo(400);
		verify(authService, never()).register(any(RegisterRequest.class));
	}
}

