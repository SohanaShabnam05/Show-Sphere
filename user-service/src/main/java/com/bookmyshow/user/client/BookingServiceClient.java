package com.bookmyshow.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "booking-service", configuration = FeignJwtForwardConfig.class)
public interface BookingServiceClient {

	@GetMapping("/api/v1/bookings/my")
	List<Object> getMyBookings();
}
