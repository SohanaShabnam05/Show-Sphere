package com.bookmyshow.event.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestCacheConfig {
	@Bean
	CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}
}

