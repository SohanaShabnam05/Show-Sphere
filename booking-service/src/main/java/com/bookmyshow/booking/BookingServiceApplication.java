package com.bookmyshow.booking;

import com.bookmyshow.booking.constant.BookingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for BookMyShow Booking Service.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Slf4j
public class BookingServiceApplication {

	public static void main(String[] args) {

		log.info(BookingConstants.LOG_APPLICATION_STARTUP);
		SpringApplication.run(BookingServiceApplication.class, args);
	}
}
