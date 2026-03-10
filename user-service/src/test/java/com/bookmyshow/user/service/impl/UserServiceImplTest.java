package com.bookmyshow.user.service.impl;

import com.bookmyshow.user.client.BookingServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private BookingServiceClient bookingServiceClient;

	@InjectMocks
	private UserServiceImpl userService;

	@Test
	void testGetMyBookings_clientReturnsList_shouldReturnSameList() {
		// Given
		List<Object> bookings = List.of(new Object(), new Object());
		when(bookingServiceClient.getMyBookings()).thenReturn(bookings);

		// When
		List<Object> result = userService.getMyBookings();

		// Then
		assertThat(result).isSameAs(bookings);
		assertThat(result).hasSize(2);
	}

	@Test
	void testGetMyBookings_clientReturnsEmpty_shouldReturnEmptyList() {
		// Given
		when(bookingServiceClient.getMyBookings()).thenReturn(List.of());

		// When
		List<Object> result = userService.getMyBookings();

		// Then
		assertThat(result).isEmpty();

		// Verify
		verify(bookingServiceClient).getMyBookings();
	}

	@Test
	void testGetMyBookings_clientReturnsNull_shouldReturnNullFromClient() {
		// Given
		when(bookingServiceClient.getMyBookings()).thenReturn(null);

		// When
		List<Object> result = userService.getMyBookings();

		// Then
		assertThat(result).isNull();

		// Verify
		verify(bookingServiceClient).getMyBookings();
	}
}

