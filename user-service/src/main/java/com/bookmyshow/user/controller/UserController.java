package com.bookmyshow.user.controller;

import com.bookmyshow.user.constant.UserConstants;
import com.bookmyshow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(UserConstants.API_V1_USERS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User and my bookings APIs")
public class UserController {

	private final UserService userService;

	@GetMapping(UserConstants.MY_BOOKINGS)
	@Operation(summary = "Get my upcoming and past bookings (requires Authorization: Bearer token)")
	public ResponseEntity<List<Object>> getMyBookings() {

		List<Object> bookings = userService.getMyBookings();
		return ResponseEntity.ok(bookings);
	}
}
