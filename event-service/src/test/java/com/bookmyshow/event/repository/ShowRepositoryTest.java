package com.bookmyshow.event.repository;

import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.entity.EventCategory;
import com.bookmyshow.event.entity.Show;
import com.bookmyshow.event.entity.Theater;
import com.bookmyshow.event.config.TestCacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestCacheConfig.class)
class ShowRepositoryTest {

	@Autowired
	private ShowRepository showRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private TheaterRepository theaterRepository;

	@Test
	void findOverlappingShows_overlaps_shouldReturnMatches() {
		// Given
		Event event = eventRepository.save(Event.builder().name("Movie").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build());
		Theater theater = theaterRepository.save(Theater.builder().name("PVR").address("Addr").build());

		LocalDateTime start = LocalDateTime.now().plusDays(1);
		LocalDateTime end = start.plusHours(2);
		showRepository.save(Show.builder().event(event).theater(theater).startTime(start).endTime(end).totalSeats(100).availableSeats(100).build());

		// When
		List<Show> overlaps = showRepository.findOverlappingShows(theater.getId(), event.getId(), start.plusMinutes(30), end.plusMinutes(30));

		// THEN
		assertThat(overlaps).hasSize(1);
	}

	@Test
	void searchShows_filtersByCategoryAndEventName_shouldReturnMatches() {
		// Given
		Event movie = eventRepository.save(Event.builder().name("Avengers").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build());
		Event concert = eventRepository.save(Event.builder().name("Rock").category(EventCategory.CONCERT).basePrice(BigDecimal.TEN).build());
		Theater t1 = theaterRepository.save(Theater.builder().name("PVR").address("Addr").build());

		LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
		showRepository.save(Show.builder().event(movie).theater(t1).startTime(start).endTime(start.plusHours(2)).totalSeats(100).availableSeats(100).build());
		showRepository.save(Show.builder().event(concert).theater(t1).startTime(start).endTime(start.plusHours(2)).totalSeats(100).availableSeats(100).build());

		// When
		List<Show> results = showRepository.searchShows(EventCategory.MOVIE, start.toLocalDate(), null, "aven");

		// THEN
		assertThat(results).hasSize(1);
		assertThat(results.get(0).getEvent().getName()).containsIgnoringCase("Avengers");
	}

	@Test
	void findAllWithEventAndTheater_shouldFetchAssociations() {
		// Given
		Event event = eventRepository.save(Event.builder().name("Movie").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build());
		Theater theater = theaterRepository.save(Theater.builder().name("PVR").address("Addr").build());
		LocalDateTime start = LocalDate.now().plusDays(3).atStartOfDay();
		showRepository.save(Show.builder().event(event).theater(theater).startTime(start).endTime(start.plusHours(2)).totalSeats(10).availableSeats(10).build());

		// When
		List<Show> shows = showRepository.findAllWithEventAndTheater();

		// THEN
		assertThat(shows).isNotEmpty();
		assertThat(shows.get(0).getEvent().getName()).isEqualTo("Movie");
		assertThat(shows.get(0).getTheater().getName()).isEqualTo("PVR");
	}
}

