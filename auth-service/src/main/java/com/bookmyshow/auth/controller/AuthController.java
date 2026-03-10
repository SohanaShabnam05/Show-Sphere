package com.bookmyshow.auth.controller;

import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.dto.JwtResponse;
import com.bookmyshow.auth.dto.LoginRequest;
import com.bookmyshow.auth.dto.RegisterRequest;
import com.bookmyshow.auth.dto.UserSummaryResponse;
import com.bookmyshow.auth.entity.User;
import com.bookmyshow.auth.repository.UserRepository;
import com.bookmyshow.auth.service.AuthService;
import com.bookmyshow.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for user registration and login. Issues JWT on success.
 */
@RestController
@RequestMapping(AuthConstants.API_V1_AUTH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "Registration and Login APIs")
public class AuthController {

	private final AuthService authService;
	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;

	/**
	 *
	 * @param request
	 * @return
	 */
	@PostMapping(AuthConstants.REGISTER)
	@Operation(
			summary = "Register a new user",
			description = "Validates: email format, mobile exactly 10 digits, DOB required and past. Only users 18+ allowed. Returns JWT on success. Throws custom exceptions for validation failures (400).")
	public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {

		JwtResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 *
	 * @param request
	 * @return
	 */
	@PostMapping(AuthConstants.LOGIN)
	@Operation(summary = "Login and get JWT")
	public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {

		JwtResponse response = authService.login(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping(AuthConstants.USERS)
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@Operation(summary = "Get all users (any authenticated user)")
	public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {

		List<User> users = userRepository.findAll();
		List<UserSummaryResponse> response = users.stream()
				.map(user -> UserSummaryResponse.builder()
						.id(user.getId())
						.name(user.getName())
						.email(user.getEmail())
						.mobile(user.getMobile())
						.address(user.getAddress())
						.dateOfBirth(user.getDateOfBirth())
						.role(user.getRole())
						.build())
				.toList();
		return ResponseEntity.ok(response);
	}
}
