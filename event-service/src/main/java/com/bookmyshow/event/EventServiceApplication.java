package com.bookmyshow.event;

import com.bookmyshow.event.constant.EventConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for BookMyShow Event Service. Manages events, theaters and shows.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
@Slf4j
public class EventServiceApplication {

	/**
	 * Starts the Event Service Spring Boot application.
	 *
	 * @param args standard application arguments
	 */
	public static void main(String[] args) {

		log.info(EventConstants.LOG_APPLICATION_STARTUP);

		SpringApplication.run(EventServiceApplication.class, args);
	}
}
