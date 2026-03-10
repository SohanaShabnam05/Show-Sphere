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
import com.bookmyshow.auth.util.JwtUtil;
import com.bookmyshow.auth.util.RegistrationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private AuthServiceImpl authService;

	@Test
	void register_validRequest_shouldCreateUserAndReturnJwtResponse() {
		// Given
		RegisterRequest request = RegisterRequest.builder()
				.name("Test User")
				.email("user@example.com")
				.password("password")
				.mobile("1234567890")
				.address("Address")
				.dateOfBirth(LocalDate.now().minusYears(AuthConstants.MINIMUM_AGE_YEARS + 1))
				.build();

		when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
		when(passwordEncoder.encode("password")).thenReturn("encoded");

		User savedUser = User.builder()
				.id(1L)
				.name(request.getName())
				.email(request.getEmail())
				.password("encoded")
				.mobile(request.getMobile())
				.address(request.getAddress())
				.dateOfBirth(request.getDateOfBirth())
				.role(Role.ROLE_USER)
				.build();
		when(userRepository.save(any(User.class))).thenReturn(savedUser);
		when(jwtUtil.generateToken(savedUser)).thenReturn("jwt-token");

		// When
		JwtResponse response = authService.register(request);

		// THEN
		assertThat(response.getToken()).isEqualTo("jwt-token");
		assertThat(response.getUserId()).isEqualTo(1L);
		assertThat(response.getEmail()).isEqualTo("user@example.com");
		assertThat(response.getRole()).isEqualTo(Role.ROLE_USER.name());

		// verify
		verify(userRepository).existsByEmail(request.getEmail());
		verify(userRepository).save(any(User.class));
		verify(jwtUtil).generateToken(savedUser);
	}

	@Test
	void register_emailAlreadyExists_shouldThrowInvalidEmailException() {
		// Given
		RegisterRequest request = RegisterRequest.builder()
				.name("Test User")
				.email("user@example.com")
				.password("password")
				.mobile("1234567890")
				.address("Address")
				.dateOfBirth(LocalDate.now().minusYears(AuthConstants.MINIMUM_AGE_YEARS + 1))
				.build();
		when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

		// When / THEN
		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(InvalidEmailException.class)
				.hasMessage(AuthConstants.ERROR_EMAIL_EXISTS);
	}

	@Test
	void login_validCredentials_shouldReturnJwtResponse() {
		// Given
		LoginRequest request = new LoginRequest("user@example.com", "rawPassword");
		User user = new User();
		user.setId(5L);
		user.setEmail(request.getEmail());
		user.setPassword("encodedPassword");
		user.setRole(Role.ROLE_ADMIN);

		when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
		when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

		// When
		JwtResponse response = authService.login(request);

		// THEN
		assertThat(response.getToken()).isEqualTo("jwt-token");
		assertThat(response.getUserId()).isEqualTo(5L);
		assertThat(response.getRole()).isEqualTo(Role.ROLE_ADMIN.name());

		// verify
		verify(userRepository).findByEmail(request.getEmail());
		verify(passwordEncoder).matches("rawPassword", "encodedPassword");
		verify(jwtUtil).generateToken(user);
	}

	@Test
	void login_unknownEmail_shouldThrowAuthServiceException() {
		// Given
		LoginRequest request = new LoginRequest("missing@example.com", "pwd");
		when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

		// When / THEN
		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(AuthServiceException.class)
				.hasMessage(AuthConstants.ERROR_INVALID_CREDENTIALS);
	}

	@Test
	void login_wrongPassword_shouldThrowAuthServiceException() {
		// Given
		LoginRequest request = new LoginRequest("user@example.com", "wrong");
		User user = new User();
		user.setEmail(request.getEmail());
		user.setPassword("encoded");

		when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

		// When / THEN
		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(AuthServiceException.class)
				.hasMessage(AuthConstants.ERROR_INVALID_CREDENTIALS);
	}
}

