package com.bookmyshow.event.controller;

import com.bookmyshow.event.config.JwtFilter;
import com.bookmyshow.event.config.SecurityConfig;
import com.bookmyshow.event.config.TestCacheConfig;
import com.bookmyshow.event.dto.ReleaseSeatsResponse;
import com.bookmyshow.event.dto.ShowDetailsDto;
import com.bookmyshow.event.dto.ShowRequest;
import com.bookmyshow.event.entity.EventCategory;
import com.bookmyshow.event.entity.Show;
import com.bookmyshow.event.service.ShowService;
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

import java.time.LocalDateTime;
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

@WebMvcTest(controllers = ShowController.class)
@Import({ SecurityConfig.class, TestCacheConfig.class })
class ShowControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ShowService showService;

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
	void getShowDetails_unauthenticated_shouldReturn200() throws Exception {
		// Given
		ShowDetailsDto details = ShowDetailsDto.builder()
				.id(1L)
				.eventId(2L)
				.eventCategory(EventCategory.MOVIE)
				.availableSeats(10)
				.startTime(LocalDateTime.now())
				.build();
		when(showService.getShowDetails(1L)).thenReturn(details);

		// When
		var result = mockMvc.perform(get("/api/v1/events/shows/1/details"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void reserveSeats_unauthenticated_shouldReturn200() throws Exception {
		// Given
		when(showService.reserveSeats(5L, 2)).thenReturn(true);

		// When
		var result = mockMvc.perform(post("/api/v1/events/shows/5/reserve?count=2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").value(true))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void releaseSeats_unauthenticated_shouldReturn200() throws Exception {
		// Given
		ReleaseSeatsResponse response = ReleaseSeatsResponse.builder().releasedCount(2).message("2 seats have been released.").build();
		when(showService.releaseSeats(6L, 2)).thenReturn(response);

		// When
		var result = mockMvc.perform(post("/api/v1/events/shows/6/release?count=2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releasedCount").value(2))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void createShow_unauthenticated_shouldReturn401() throws Exception {
		// When
		var result = mockMvc.perform(post("/api/v1/events/shows")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void createShow_userRole_shouldReturn403() throws Exception {
		// Given
		ShowRequest request = ShowRequest.builder()
				.eventId(1L).theaterId(2L)
				.startTime(LocalDateTime.now().plusHours(1))
				.endTime(LocalDateTime.now().plusHours(2))
				.totalSeats(10)
				.build();

		// When
		var result = mockMvc.perform(post("/api/v1/events/shows")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(user("u").roles("USER")))
				.andExpect(status().isForbidden())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void createShow_adminRole_shouldReturn201() throws Exception {
		// Given
		ShowRequest request = ShowRequest.builder()
				.eventId(1L).theaterId(2L)
				.startTime(LocalDateTime.now().plusHours(1))
				.endTime(LocalDateTime.now().plusHours(2))
				.totalSeats(10)
				.build();
		Show created = Show.builder().id(10L).totalSeats(10).availableSeats(10).build();
		when(showService.createShow(any(ShowRequest.class))).thenReturn(created);

		// When
		var result = mockMvc.perform(post("/api/v1/events/shows")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(10))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(201);
	}

	@Test
	void getAllShows_unauthenticated_shouldReturn401() throws Exception {
		// When
		var result = mockMvc.perform(get("/api/v1/events/shows"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void getAllShows_authenticated_shouldReturn200() throws Exception {
		// Given
		when(showService.getAllShows()).thenReturn(List.of());

		// When
		var result = mockMvc.perform(get("/api/v1/events/shows").with(user("u").roles("USER")))
				.andExpect(status().isOk())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}
}

