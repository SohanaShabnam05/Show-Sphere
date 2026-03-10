package com.bookmyshow.auth.service.impl;

import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.dto.JwtResponse;
import com.bookmyshow.auth.dto.LoginRequest;
import com.bookmyshow.auth.dto.RegisterRequest;
import com.bookmyshow.auth.entity.Role;
import com.bookmyshow.auth.entity.User;
import com.bookmyshow.auth.exception.AuthServiceException;
import com.bookmyshow.auth.exception.InvalidEmailException;
import com.bookmyshow.auth.repository.UserRepository;
import com.bookmyshow.auth.service.AuthService;
import com.bookmyshow.auth.util.JwtUtil;
import com.bookmyshow.auth.util.RegistrationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService}. Handles registration and login with JWT issuance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	@Override
	@Transactional
	public JwtResponse register(RegisterRequest request) {

		log.debug("Register attempt for email: {}", request.getEmail());

		RegistrationValidator.validateRegistrationRequest(request);

		if (Boolean.TRUE.equals(userRepository.existsByEmail(request.getEmail()))) {
			throw new InvalidEmailException(AuthConstants.ERROR_EMAIL_EXISTS);
		}

		User user = User.builder()
				.name(request.getName())
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.mobile(request.getMobile())
				.address(request.getAddress())
				.dateOfBirth(request.getDateOfBirth())
				.role(Role.ROLE_USER)
				.build();
		user = userRepository.save(user);

		log.info(AuthConstants.LOG_USER_REGISTERED);

		String token = jwtUtil.generateToken(user);
		return buildJwtResponse(token, user);
	}

	@Override
	public JwtResponse login(LoginRequest request) {

		log.debug("Login attempt for email: {}", request.getEmail());

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new AuthServiceException(AuthConstants.ERROR_INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new AuthServiceException(AuthConstants.ERROR_INVALID_CREDENTIALS);
		}

		log.info(AuthConstants.LOG_LOGIN_SUCCESS);

		String token = jwtUtil.generateToken(user);
		return buildJwtResponse(token, user);
	}

	private JwtResponse buildJwtResponse(String token, User user) {

		return JwtResponse.builder()
				.token(token)
				.type(AuthConstants.BEARER_PREFIX.trim())
				.userId(user.getId())
				.email(user.getEmail())
				.role(user.getRole().name())
				.build();
	}
}
