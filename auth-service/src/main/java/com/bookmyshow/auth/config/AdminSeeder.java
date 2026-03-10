package com.bookmyshow.auth.config;

import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.entity.Role;
import com.bookmyshow.auth.entity.User;
import com.bookmyshow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Seeds exactly one admin user if it does not already exist. Idempotent and production-safe.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(String... args) {

		if (Boolean.TRUE.equals(userRepository.existsByEmail(AuthConstants.ADMIN_SEED_EMAIL))) {
			log.debug("Admin user already exists, skipping seed.");
			return;
		}

		User admin = User.builder()
				.name("System Admin")
				.email(AuthConstants.ADMIN_SEED_EMAIL)
				.password(passwordEncoder.encode(AuthConstants.ADMIN_SEED_PASSWORD))
				.mobile("0000000000")
				.address(null)
				.dateOfBirth(LocalDate.of(1990, 1, 1))
				.role(Role.ROLE_ADMIN)
				.build();
		userRepository.save(admin);
		log.info("Seeded single admin user: {}", AuthConstants.ADMIN_SEED_EMAIL);
	}
}
