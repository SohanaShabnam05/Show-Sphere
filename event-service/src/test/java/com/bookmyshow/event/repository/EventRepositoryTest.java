package com.bookmyshow.event.repository;

import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.entity.EventCategory;
import com.bookmyshow.event.config.TestCacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestCacheConfig.class)
class EventRepositoryTest {

	@Autowired
	private EventRepository eventRepository;

	@Test
	void findByCategory_shouldReturnOnlyMatchingEvents() {
		// Given
		eventRepository.save(Event.builder().name("M1").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build());
		eventRepository.save(Event.builder().name("C1").category(EventCategory.CONCERT).basePrice(BigDecimal.TEN).build());

		// When
		var movies = eventRepository.findByCategory(EventCategory.MOVIE);

		// THEN
		assertThat(movies).hasSize(1);
		assertThat(movies.get(0).getCategory()).isEqualTo(EventCategory.MOVIE);
	}
}

