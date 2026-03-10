package com.bookmyshow.booking.client;

import com.bookmyshow.booking.dto.ReleaseSeatsResponseDto;
import com.bookmyshow.booking.dto.ShowDetailsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for Event Service (show details, reserve/release seats).
 */
@FeignClient(name = "event-service", configuration = com.bookmyshow.booking.config.FeignJwtForwardConfig.class)
public interface EventServiceClient {

	@GetMapping("/api/v1/events/shows/{showId}/details")
	ShowDetailsDto getShowDetails(@PathVariable("showId") Long showId);

	@PostMapping("/api/v1/events/shows/{showId}/reserve")
	boolean reserveSeats(@PathVariable("showId") Long showId, @RequestParam("count") int count);

	@PostMapping("/api/v1/events/shows/{showId}/release")
	ReleaseSeatsResponseDto releaseSeats(@PathVariable("showId") Long showId, @RequestParam("count") int count);
}
