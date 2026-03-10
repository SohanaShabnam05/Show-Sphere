package com.bookmyshow.auth.service;

import com.bookmyshow.auth.dto.JwtResponse;
import com.bookmyshow.auth.dto.LoginRequest;
import com.bookmyshow.auth.dto.RegisterRequest;

/**
 * Service contract for user registration and authentication.
 */
public interface AuthService {

	/**
	 * Registers a new user with USER role.
	 *
	 * @param request registration payload
	 * @return JWT response with token and user info
	 */
	JwtResponse register(RegisterRequest request);

	/**
	 * Authenticates user and returns JWT.
	 *
	 * @param request login credentials
	 * @return JWT response with token and user info
	 */
	JwtResponse login(LoginRequest request);
}
