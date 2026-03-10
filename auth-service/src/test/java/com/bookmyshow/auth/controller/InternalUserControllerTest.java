package com.bookmyshow.auth.controller;

import com.bookmyshow.auth.dto.UserDobResponse;
import com.bookmyshow.auth.entity.User;
import com.bookmyshow.auth.entity.Role;
import com.bookmyshow.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserControllerTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private InternalUserController internalUserController;

	@Test
	void testGetUserDob_userExists_returns200WithDob() {
		// Given
		Long userId = 1L;
		User user = User.builder()
				.id(userId)
				.name("Test")
				.email("test@example.com")
				.dateOfBirth(LocalDate.of(1990, 5, 15))
				.role(Role.ROLE_USER)
				.build();
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		// When
		ResponseEntity<UserDobResponse> response = internalUserController.getUserDob(userId);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));

		// Verify
		verify(userRepository).findById(userId);
	}

	@Test
	void testGetUserDob_userNotFound_returns404() {
		// Given
		Long userId = 999L;
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// When
		ResponseEntity<UserDobResponse> response = internalUserController.getUserDob(userId);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNull();

		// Verify
		verify(userRepository).findById(userId);
	}
}
