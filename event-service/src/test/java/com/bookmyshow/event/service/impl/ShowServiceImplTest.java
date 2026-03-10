package com.bookmyshow.event.service.impl;

import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.dto.ReleaseSeatsResponse;
import com.bookmyshow.event.dto.ShowDetailsDto;
import com.bookmyshow.event.dto.ShowRequest;
import com.bookmyshow.event.dto.ShowResponseDto;
import com.bookmyshow.event.dto.ShowSearchRequest;
import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.entity.EventCategory;
import com.bookmyshow.event.entity.Show;
import com.bookmyshow.event.entity.Theater;
import com.bookmyshow.event.exception.EventServiceException;
import com.bookmyshow.event.repository.EventRepository;
import com.bookmyshow.event.repository.ShowRepository;
import com.bookmyshow.event.repository.TheaterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowServiceImplTest {

	@Mock
	private ShowRepository showRepository;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private TheaterRepository theaterRepository;

	@InjectMocks
	private ShowServiceImpl showService;

	@Test
	void createShow_eventMissing_shouldThrowEventServiceException() {
		// Given
		ShowRequest request = ShowRequest.builder()
				.eventId(1L)
				.theaterId(2L)
				.startTime(LocalDateTime.now().plusHours(1))
				.endTime(LocalDateTime.now().plusHours(2))
				.totalSeats(100)
				.build();
		when(eventRepository.findById(1L)).thenReturn(Optional.empty());

		// When
		Throwable thrown = catchThrowable(() -> showService.createShow(request));

		// THEN
		assertThat(thrown).isInstanceOf(EventServiceException.class)
				.hasMessage(EventConstants.ERROR_EVENT_NOT_FOUND);
	}

	@Test
	void createShow_theaterMissing_shouldThrowEventServiceException() {
		// Given
		ShowRequest request = ShowRequest.builder()
				.eventId(1L)
				.theaterId(2L)
				.startTime(LocalDateTime.now().plusHours(1))
				.endTime(LocalDateTime.now().plusHours(2))
				.totalSeats(100)
				.build();
		when(eventRepository.findById(1L)).thenReturn(Optional.of(Event.builder().id(1L).build()));
		when(theaterRepository.findById(2L)).thenReturn(Optional.empty());

		// When
		Throwable thrown = catchThrowable(() -> showService.createShow(request));

		// THEN
		assertThat(thrown).isInstanceOf(EventServiceException.class)
				.hasMessage(EventConstants.ERROR_THEATER_NOT_FOUND);
	}

	@Test
	void createShow_overlappingShows_shouldThrowEventServiceException() {
		// Given
		ShowRequest request = ShowRequest.builder()
				.eventId(1L)
				.theaterId(2L)
				.startTime(LocalDateTime.now().plusHours(1))
				.endTime(LocalDateTime.now().plusHours(2))
				.totalSeats(100)
				.build();
		when(eventRepository.findById(1L)).thenReturn(Optional.of(Event.builder().id(1L).build()));
		when(theaterRepository.findById(2L)).thenReturn(Optional.of(Theater.builder().id(2L).build()));
		when(showRepository.findOverlappingShows(2L, 1L, request.getStartTime(), request.getEndTime()))
				.thenReturn(List.of(Show.builder().id(9L).build()));

		// When
		Throwable thrown = catchThrowable(() -> showService.createShow(request));

		// THEN
		assertThat(thrown).isInstanceOf(EventServiceException.class)
				.hasMessage(EventConstants.ERROR_SHOW_OVERLAP);
	}

	@Test
	void createShow_validRequest_shouldSaveWithAvailableSeatsEqualTotalSeats() {
		// Given
		LocalDateTime start = LocalDateTime.now().plusHours(1);
		LocalDateTime end = start.plusHours(2);
		ShowRequest request = ShowRequest.builder()
				.eventId(1L)
				.theaterId(2L)
				.startTime(start)
				.endTime(end)
				.totalSeats(50)
				.build();
		Event event = Event.builder().id(1L).name("Movie").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build();
		Theater theater = Theater.builder().id(2L).name("PVR").address("Addr").build();
		when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
		when(theaterRepository.findById(2L)).thenReturn(Optional.of(theater));
		when(showRepository.findOverlappingShows(2L, 1L, start, end)).thenReturn(List.of());

		when(showRepository.save(any(Show.class))).thenAnswer(inv -> {
			Show s = inv.getArgument(0);
			s.setId(10L);
			return s;
		});

		ArgumentCaptor<Show> captor = ArgumentCaptor.forClass(Show.class);

		// When
		Show saved = showService.createShow(request);

		// THEN
		assertThat(saved.getId()).isEqualTo(10L);
		verify(showRepository).save(captor.capture());
		Show toSave = captor.getValue();
		assertThat(toSave.getAvailableSeats()).isEqualTo(50);
		assertThat(toSave.getTotalSeats()).isEqualTo(50);
		assertThat(toSave.getEvent()).isSameAs(event);
		assertThat(toSave.getTheater()).isSameAs(theater);
	}

	@Test
	void getAllShows_shouldClampAvailableSeatsForNegativeOrOverflow() {
		// Given
		Event event = Event.builder().id(1L).name("Movie").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build();
		Theater theater = Theater.builder().id(2L).name("PVR").address("Addr").build();
		Show negative = Show.builder().id(1L).event(event).theater(theater).totalSeats(100).availableSeats(-5)
				.startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).build();
		Show overflow = Show.builder().id(2L).event(event).theater(theater).totalSeats(100).availableSeats(150)
				.startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).build();
		when(showRepository.findAllWithEventAndTheater()).thenReturn(List.of(negative, overflow));

		// When
		List<ShowResponseDto> result = showService.getAllShows();

		// THEN
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getAvailableSeats()).isEqualTo(0);
		assertThat(result.get(1).getAvailableSeats()).isEqualTo(100);
	}

	@Test
	void getShowDetails_shouldClampAvailableSeats() {
		// Given
		Event event = Event.builder().id(1L).name("Movie").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build();
		Theater theater = Theater.builder().id(2L).name("PVR").address("Addr").build();
		Show show = Show.builder().id(3L).event(event).theater(theater).totalSeats(10).availableSeats(-1)
				.startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).build();
		when(showRepository.findById(3L)).thenReturn(Optional.of(show));

		// When
		ShowDetailsDto details = showService.getShowDetails(3L);

		// THEN
		assertThat(details.getAvailableSeats()).isEqualTo(0);
		assertThat(details.getEventCategory()).isEqualTo(EventCategory.MOVIE);
	}

	@Test
	void reserveSeats_insufficientSeats_shouldReturnFalseAndNotSave() {
		// Given
		Show show = Show.builder().id(1L).totalSeats(10).availableSeats(2).build();
		when(showRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(show));

		// When
		boolean ok = showService.reserveSeats(1L, 3);

		// THEN
		assertThat(ok).isFalse();
		verify(showRepository, never()).save(any());
	}

	@Test
	void reserveSeats_success_shouldDecrementAndSave() {
		// Given
		Show show = Show.builder().id(1L).totalSeats(10).availableSeats(5).build();
		when(showRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(show));

		// When
		boolean ok = showService.reserveSeats(1L, 2);

		// THEN
		assertThat(ok).isTrue();
		assertThat(show.getAvailableSeats()).isEqualTo(3);
		verify(showRepository).save(show);
	}

	@Test
	void releaseSeats_exceedsReserved_shouldThrowEventServiceException() {
		// Given
		Show show = Show.builder().id(1L).totalSeats(10).availableSeats(9).build(); // reserved=1
		when(showRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(show));

		// When
		Throwable thrown = catchThrowable(() -> showService.releaseSeats(1L, 2));

		// THEN
		assertThat(thrown).isInstanceOf(EventServiceException.class)
				.hasMessage(String.format(EventConstants.ERROR_RELEASE_EXCEEDS_RESERVED, 1, 2));
	}

	@Test
	void releaseSeats_negativeAvailable_shouldRepairAndReturnMessage() {
		// Given
		Show show = Show.builder().id(1L).totalSeats(10).availableSeats(-5).build(); // effective=0 reserved=10
		when(showRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(show));

		// When
		ReleaseSeatsResponse response = showService.releaseSeats(1L, 1);

		// THEN
		assertThat(response.getReleasedCount()).isEqualTo(1);
		assertThat(response.getMessage()).isEqualTo("1 seat has been released.");
		assertThat(show.getAvailableSeats()).isEqualTo(1);
		verify(showRepository).save(show);
	}

	@Test
	void repairShowAvailability_overflowAvailable_shouldClampAndSave() {
		// Given
		Show show = Show.builder().id(1L).totalSeats(10).availableSeats(50).build();
		when(showRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(show));

		// When
		showService.repairShowAvailability(1L);

		// THEN
		assertThat(show.getAvailableSeats()).isEqualTo(10);
		verify(showRepository).save(show);
	}

	@Test
	void searchShows_shouldDelegateToRepository() {
		// Given
		ShowSearchRequest request = ShowSearchRequest.builder()
				.category(EventCategory.MOVIE)
				.theaterId(2L)
				.eventName("avengers")
				.showDate(LocalDate.now())
				.build();
		Event event = Event.builder().id(1L).name("Avengers").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build();
		Theater theater = Theater.builder().id(2L).name("PVR").address("Addr").build();
		Show show = Show.builder().id(3L).event(event).theater(theater).totalSeats(10).availableSeats(10)
				.startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).build();
		when(showRepository.searchShows(any(), any(), any(), any())).thenReturn(List.of(show));

		// When
		List<ShowResponseDto> result = showService.searchShows(request);

		// THEN
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getEventName()).isEqualTo("Avengers");
		verify(showRepository).searchShows(EventCategory.MOVIE, request.getShowDate(), 2L, "avengers");
	}
}

