package com.bookmyshow.auth.controller;

import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.dto.UserDobResponse;
import com.bookmyshow.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoints for other services (e.g. booking-service for user DOB / GST age check).
 */
@RestController
@RequestMapping(AuthConstants.INTERNAL_USERS)
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Internal APIs for other microservices")
public class InternalUserController {

	private final UserRepository userRepository;

	@GetMapping("/{id}/dob")
	@Operation(summary = "Get user date of birth (internal)")
	public ResponseEntity<UserDobResponse> getUserDob(@PathVariable Long id) {

		return userRepository.findById(id)
				.map(user -> ResponseEntity.ok(UserDobResponse.builder()
						.dateOfBirth(user.getDateOfBirth())
						.build()))
				.orElse(ResponseEntity.notFound().build());
	}
}
