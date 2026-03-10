package com.bookmyshow.event.repository;

import com.bookmyshow.event.config.TestCacheConfig;
import com.bookmyshow.event.entity.Theater;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestCacheConfig.class)
class TheaterRepositoryTest {

	@Autowired
	private TheaterRepository theaterRepository;

	@Test
	void testSave_andFindById_shouldPersistAndRetrieve() {
		// Given
		Theater theater = Theater.builder()
				.name("PVR")
				.address("Mall Road")
				.build();

		// When
		Theater saved = theaterRepository.save(theater);
		Theater found = theaterRepository.findById(saved.getId()).orElse(null);

		// Then
		assertThat(saved.getId()).isNotNull();
		assertThat(found).isNotNull();
		assertThat(found.getName()).isEqualTo("PVR");
		assertThat(found.getAddress()).isEqualTo("Mall Road");
		// Verify
		assertThat(theaterRepository.count()).isEqualTo(1);
	}

	@Test
	void testFindAll_shouldReturnAllSaved() {
		// Given
		theaterRepository.save(Theater.builder().name("A").address("Addr1").build());
		theaterRepository.save(Theater.builder().name("B").address("Addr2").build());

		// When
		var all = theaterRepository.findAll();

		// Then
		assertThat(all).hasSize(2);
		// Verify
	}
}
