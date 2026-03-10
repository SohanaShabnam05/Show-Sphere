package com.bookmyshow.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for Booking Service. Used to check if event has associated bookings before delete.
 */
@FeignClient(name = "booking-service")
public interface BookingServiceClient {

	@GetMapping("/api/v1/bookings/has-bookings-by-show/{showId}")
	Map<String, Boolean> hasBookingsByShowId(@PathVariable("showId") Long showId);
}
