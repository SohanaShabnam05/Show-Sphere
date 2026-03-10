package com.bookmyshow.event.controller;

import com.bookmyshow.event.config.JwtFilter;
import com.bookmyshow.event.config.SecurityConfig;
import com.bookmyshow.event.config.TestCacheConfig;
import com.bookmyshow.event.dto.EventRequest;
import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.entity.EventCategory;
import com.bookmyshow.event.service.EventService;
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

import java.math.BigDecimal;
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

@WebMvcTest(controllers = EventController.class)
@Import({ SecurityConfig.class, TestCacheConfig.class })
class EventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private EventService eventService;

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
	void createEvent_unauthenticated_shouldReturn401() throws Exception {
		// When
		var result = mockMvc.perform(post("/api/v1/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void createEvent_userRole_shouldReturn403() throws Exception {
		// Given
		EventRequest request = EventRequest.builder()
				.name("Movie")
				.category(EventCategory.MOVIE)
				.basePrice(new BigDecimal("100.00"))
				.build();

		// When
		var result = mockMvc.perform(post("/api/v1/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(user("u").roles("USER")))
				.andExpect(status().isForbidden())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void createEvent_adminRole_shouldReturn201() throws Exception {
		// Given
		EventRequest request = EventRequest.builder()
				.name("Movie")
				.category(EventCategory.MOVIE)
				.basePrice(new BigDecimal("100.00"))
				.build();
		Event created = Event.builder().id(1L).name("Movie").category(EventCategory.MOVIE).basePrice(new BigDecimal("100.00")).build();
		when(eventService.createEvent(any(EventRequest.class))).thenReturn(created);

		// When
		var result = mockMvc.perform(post("/api/v1/events")
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
	void getAllEvents_authenticated_shouldReturn200() throws Exception {
		// Given
		when(eventService.getAllEvents()).thenReturn(List.of(Event.builder().id(1L).name("A").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build()));

		// When
		var result = mockMvc.perform(get("/api/v1/events").with(user("u").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}
}

