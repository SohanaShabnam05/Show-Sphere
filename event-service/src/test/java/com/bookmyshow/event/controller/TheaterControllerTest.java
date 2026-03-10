package com.bookmyshow.event.controller;

import com.bookmyshow.event.config.JwtFilter;
import com.bookmyshow.event.config.SecurityConfig;
import com.bookmyshow.event.config.TestCacheConfig;
import com.bookmyshow.event.dto.TheaterRequest;
import com.bookmyshow.event.entity.Theater;
import com.bookmyshow.event.service.TheaterService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TheaterController.class)
@Import({ SecurityConfig.class, TestCacheConfig.class })
class TheaterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private TheaterService theaterService;

	@MockitoBean
	private JwtFilter jwtFilter;

	@BeforeEach
	void setUp() throws Exception {
		doAnswer(invocation -> {
			ServletRequest req = invocation.getArgument(0);
			ServletResponse res = invocation.getArgument(1);
			FilterChain chain = invocation.getArgument(2);
			chain.doFilter(req, res);
			return null;
		}).when(jwtFilter).doFilter(any(), any(), any());
	}

	@Test
	void createTheater_userRole_shouldReturn403() throws Exception {
		// Given
		TheaterRequest request = TheaterRequest.builder().name("PVR").address("Addr").build();

		// When
		var result = mockMvc.perform(post("/api/v1/events/theaters")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(user("u").roles("USER")))
				.andExpect(status().isForbidden())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void createTheater_adminRole_shouldReturn201() throws Exception {
		// Given
		TheaterRequest request = TheaterRequest.builder().name("PVR").address("Addr").build();
		Theater created = Theater.builder().id(1L).name("PVR").address("Addr").build();
		when(theaterService.createTheater(any(TheaterRequest.class))).thenReturn(created);

		// When
		var result = mockMvc.perform(post("/api/v1/events/theaters")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(201);
	}

	@Test
	void getAllTheaters_authenticated_shouldReturn200() throws Exception {
		// Given
		when(theaterService.getAllTheaters()).thenReturn(List.of(Theater.builder().id(1L).name("PVR").build()));

		// When
		var result = mockMvc.perform(get("/api/v1/events/theaters").with(user("u").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("PVR"))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}
}

