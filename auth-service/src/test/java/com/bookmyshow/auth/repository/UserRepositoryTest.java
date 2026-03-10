package com.bookmyshow.auth.repository;

import com.bookmyshow.auth.entity.User;
import com.bookmyshow.auth.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void testFindByEmail_userExists_returnsUser() {
		// Given
		User user = User.builder()
				.name("Test User")
				.email("findme@example.com")
				.password("encoded")
				.mobile("1234567890")
				.dateOfBirth(LocalDate.now().minusYears(25))
				.role(Role.ROLE_USER)
				.build();
		userRepository.save(user);

		// When
		Optional<User> found = userRepository.findByEmail("findme@example.com");

		// Then
		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("findme@example.com");
		assertThat(found.get().getName()).isEqualTo("Test User");

		// Verify
		assertThat(userRepository.existsByEmail("findme@example.com")).isTrue();
	}

	@Test
	void testFindByEmail_userNotExists_returnsEmpty() {
		// Given
		// When
		Optional<User> found = userRepository.findByEmail("missing@example.com");

		// Then
		assertThat(found).isEmpty();

		// Verify
		assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
	}

	@Test
	void testExistsByEmail_afterSave_returnsTrue() {
		// Given
		User user = User.builder()
				.name("Exists")
				.email("exists@example.com")
				.password("pwd")
				.dateOfBirth(LocalDate.now().minusYears(20))
				.role(Role.ROLE_USER)
				.build();
		userRepository.save(user);

		// When
		boolean exists = userRepository.existsByEmail("exists@example.com");

		// Then
		assertThat(exists).isTrue();
	}
}
