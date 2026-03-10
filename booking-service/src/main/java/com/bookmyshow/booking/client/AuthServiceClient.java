package com.bookmyshow.booking.client;

import com.bookmyshow.booking.dto.UserDobResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for Auth Service (user DOB for GST age check).
 */
@FeignClient(name = "auth-service", configuration = com.bookmyshow.booking.config.FeignJwtForwardConfig.class)
public interface AuthServiceClient {

	@GetMapping("/internal/users/{id}/dob")
	ResponseEntity<UserDobResponseDto> getUserDob(@PathVariable("id") Long userId);
}
