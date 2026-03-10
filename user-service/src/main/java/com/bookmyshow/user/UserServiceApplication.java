package com.bookmyshow.user;

import com.bookmyshow.user.constant.UserConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Slf4j
public class UserServiceApplication {

	public static void main(String[] args) {
		log.info(UserConstants.LOG_APPLICATION_STARTUP);
		SpringApplication.run(UserServiceApplication.class, args);
	}
}
