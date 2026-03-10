package com.bookmyshow.auth;

import com.bookmyshow.auth.constant.AuthConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Entry point for BookMyShow Auth Service. Handles registration, login and JWT issuance.
 */
@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class AuthServiceApplication {

	/**
	 * Starts the Auth Service Spring Boot application.
	 *
	 * @param args standard application arguments
	 */
	public static void main(String[] args) {

		log.info(AuthConstants.LOG_APPLICATION_STARTUP);

		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
