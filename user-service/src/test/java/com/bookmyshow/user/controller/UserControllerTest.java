package com.bookmyshow.user.controller;

import com.bookmyshow.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	void getMyBookings_serviceReturnsList_shouldReturn200AndJsonArray() throws Exception {
		// Given
		when(userService.getMyBookings()).thenReturn(List.of("b1", "b2"));

		// When
		var result = mockMvc.perform(get("/api/v1/users/my-bookings"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").value("b1"))
				.andReturn();

		// THEN
		assertThat(result.getResponse().getStatus()).isEqualTo(200);
	}
}

