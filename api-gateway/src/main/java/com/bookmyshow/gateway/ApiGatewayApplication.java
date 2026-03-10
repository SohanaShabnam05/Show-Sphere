package com.bookmyshow.gateway;

import com.bookmyshow.gateway.constant.GatewayConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Entry point for BookMyShow API Gateway. Routes requests to microservices and validates JWT.
 */
@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class ApiGatewayApplication {

	/**
	 * Starts the API Gateway Spring Boot application.
	 *
	 * @param args standard application arguments
	 */
	public static void main(String[] args) {

		log.info(GatewayConstants.LOG_APPLICATION_STARTUP);

		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
