package com.bookmyshow.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class ApiGatewayApplicationTest {

	@Test
	void contextLoads_shouldStartSuccessfully() {
		// Given / When - context loads
		// Then
		assertThat(Boolean.TRUE).isTrue();
	}

}
