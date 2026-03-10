package com.bookmyshow.coupon;

import com.bookmyshow.coupon.constant.CouponConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@Slf4j
public class CouponServiceApplication {

	public static void main(String[] args) {
		log.info(CouponConstants.LOG_APPLICATION_STARTUP);
		SpringApplication.run(CouponServiceApplication.class, args);
	}
}
