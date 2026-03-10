package com.bookmyshow.booking.controller;

import com.bookmyshow.booking.dto.BookingRequest;
import com.bookmyshow.booking.dto.CancelBookingResponse;
import com.bookmyshow.booking.entity.Booking;
import com.bookmyshow.booking.config.JwtFilter;
import com.bookmyshow.booking.config.SecurityConfig;
import com.bookmyshow.booking.security.UserPrincipal;
import com.bookmyshow.booking.service.BookingService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private BookingService bookingService;

	@MockitoBean
	private JwtFilter jwtFilter;

	@BeforeEach
	void setUp() throws Exception {
		// Allow request processing to continue; this filter sits in the security chain.
		doAnswer(invocation -> {
			ServletRequest req = invocation.getArgument(0);
			ServletResponse res = invocation.getArgument(1);
			FilterChain chain = invocation.getArgument(2);
			chain.doFilter(req, res);
			return null;
		}).when(jwtFilter).doFilter(any(), any(), any());
	}

	private static RequestPostProcessor authenticatedUser(Long userId) {
		UserPrincipal principal = new UserPrincipal(userId, "user@example.com");
		Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
		return authentication(auth);
	}

	@Test
	void createBooking_authenticatedUser_shouldReturn201() throws Exception {
		// Given
		BookingRequest request = BookingRequest.builder()
				.showId(10L)
				.numberOfSeats(2)
				.build();
		Booking booking = new Booking();
		booking.setId(5L);
		when(bookingService.createBooking(any(), any())).thenReturn(booking);

		String json = objectMapper.writeValueAsString(request);

		// When
		var result = mockMvc.perform(post("/api/v1/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(authenticatedUser(1L)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(5))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(201);
	}

	@Test
	void createBooking_unauthenticated_shouldReturn401() throws Exception {
		// When
		var result = mockMvc.perform(post("/api/v1/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void getMyBookings_authenticatedUser_shouldReturn200() throws Exception {
		// Given
		Booking booking = new Booking();
		booking.setId(1L);
		when(bookingService.getMyBookings(1L)).thenReturn(List.of(booking));

		// When
		var result = mockMvc.perform(get("/api/v1/bookings/my").with(authenticatedUser(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void cancelBooking_authenticatedUser_shouldReturn200() throws Exception {
		// Given
		CancelBookingResponse response = CancelBookingResponse.builder()
				.message("ok")
				.build();
		when(bookingService.cancelBooking(1L, 10L, null)).thenReturn(response);

		// When
		var result = mockMvc.perform(post("/api/v1/bookings/10/cancel").with(authenticatedUser(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("ok"))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void hasBookingsByShowId_publicEndpoint_shouldReturn200() throws Exception {
		// Given
		when(bookingService.hasBookingsByShowId(5L)).thenReturn(true);

		// When
		var result = mockMvc.perform(get("/api/v1/bookings/has-bookings-by-show/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasBookings").value(true))
				.andReturn();

		// Then
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void testGetSummaryCsv_adminRole_shouldReturn200WithCsv() throws Exception {
		// Given
		UserPrincipal principal = new UserPrincipal(1L, "admin@example.com");
		Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		when(bookingService.getReportCsv(null, null, null)).thenReturn("bookingId,userId\n1,1\nTOTALS\n");

		// When
		var result = mockMvc.perform(get("/api/v1/bookings/reports/summary.csv").with(authentication(auth)))
				.andExpect(status().isOk())
				.andReturn();

		// Then
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
		assertThat(result.getResponse().getContentAsString()).contains("TOTALS");
		assertThat(result.getResponse().getHeader("Content-Disposition")).contains("booking-summary.csv");
	}

	@Test
	void testGetSummaryCsv_userRole_shouldReturn403() throws Exception {
		// When
		var result = mockMvc.perform(get("/api/v1/bookings/reports/summary.csv").with(authenticatedUser(1L)))
				.andExpect(status().isForbidden())
				.andReturn();

		// Then
		assertThat(result.getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void getMyUpcomingBookings_authenticatedUser_shouldReturn200() throws Exception {
		// Given
		Booking booking = new Booking();
		booking.setId(2L);
		booking.setShowStartTime(LocalDateTime.now().plusDays(1));
		when(bookingService.getMyBookings(1L, true)).thenReturn(List.of(booking));

		// When
		var result = mockMvc.perform(get("/api/v1/bookings/my/upcoming").with(authenticatedUser(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(2))
				.andReturn();

		// Then
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void getMyPastBookings_authenticatedUser_shouldReturn200() throws Exception {
		// Given
		Booking booking = new Booking();
		booking.setId(3L);
		booking.setShowStartTime(LocalDateTime.now().minusDays(1));
		when(bookingService.getMyBookings(1L, false)).thenReturn(List.of(booking));

		// When
		var result = mockMvc.perform(get("/api/v1/bookings/my/past").with(authenticatedUser(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(3))
				.andReturn();

		// Then
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}
}

