package com.bookmyshow.user.service.impl;

import com.bookmyshow.user.client.BookingServiceClient;
import com.bookmyshow.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final BookingServiceClient bookingServiceClient;

	@Override
	public List<Object> getMyBookings() {

		log.debug("Fetching my bookings (identity from forwarded JWT)");
		return bookingServiceClient.getMyBookings();
	}
}
