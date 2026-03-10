package com.bookmyshow.event.service.impl;

import com.bookmyshow.event.client.BookingServiceClient;
import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.dto.EventRequest;
import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.entity.EventCategory;
import com.bookmyshow.event.entity.Show;
import com.bookmyshow.event.exception.EventServiceException;
import com.bookmyshow.event.repository.EventRepository;
import com.bookmyshow.event.repository.ShowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private ShowRepository showRepository;

	@Mock
	private BookingServiceClient bookingServiceClient;

	@InjectMocks
	private EventServiceImpl eventService;

	@Test
	void createEvent_validRequest_shouldSaveEvent() {
		// Given
		EventRequest request = EventRequest.builder()
				.name("Concert")
				.category(EventCategory.CONCERT)
				.basePrice(new BigDecimal("250.00"))
				.build();
		Event saved = Event.builder().id(5L).name("Concert").category(EventCategory.CONCERT).basePrice(new BigDecimal("250.00")).build();
		when(eventRepository.save(any(Event.class))).thenReturn(saved);

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

		// When
		Event result = eventService.createEvent(request);

		// THEN
		assertThat(result.getId()).isEqualTo(5L);
		verify(eventRepository).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("Concert");
	}

	@Test
	void deleteEvent_eventNotFound_shouldThrowEventServiceException() {
		// Given
		when(eventRepository.existsById(99L)).thenReturn(false);

		// When
		Throwable thrown = catchThrowable(() -> eventService.deleteEvent(99L));

		// THEN
		assertThat(thrown).isInstanceOf(EventServiceException.class)
				.hasMessage(EventConstants.ERROR_EVENT_NOT_FOUND);
		verify(showRepository, never()).deleteAll(any());
		verify(eventRepository, never()).deleteById(any());
	}

	@Test
	void deleteEvent_showHasBookings_shouldThrowEventServiceExceptionAndNotDelete() {
		// Given
		when(eventRepository.existsById(1L)).thenReturn(true);
		Show show = Show.builder().id(10L).startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).totalSeats(10).availableSeats(10).build();
		when(showRepository.findByEventId(1L)).thenReturn(List.of(show));
		when(bookingServiceClient.hasBookingsByShowId(10L)).thenReturn(Map.of("hasBookings", true));

		// When
		Throwable thrown = catchThrowable(() -> eventService.deleteEvent(1L));

		// THEN
		assertThat(thrown).isInstanceOf(EventServiceException.class)
				.hasMessage(EventConstants.ERROR_EVENT_HAS_BOOKINGS);
		verify(showRepository, never()).deleteAll(any());
		verify(eventRepository, never()).deleteById(any());
	}

	@Test
	void deleteEvent_noBookings_shouldDeleteShowsAndEvent() {
		// Given
		when(eventRepository.existsById(1L)).thenReturn(true);
		Show s1 = Show.builder().id(10L).startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).totalSeats(10).availableSeats(10).build();
		Show s2 = Show.builder().id(11L).startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).totalSeats(10).availableSeats(10).build();
		when(showRepository.findByEventId(1L)).thenReturn(List.of(s1, s2));
		when(bookingServiceClient.hasBookingsByShowId(10L)).thenReturn(Map.of("hasBookings", false));
		when(bookingServiceClient.hasBookingsByShowId(11L)).thenReturn(Map.of("hasBookings", false));

		// When
		eventService.deleteEvent(1L);

		// THEN
		verify(showRepository).deleteAll(List.of(s1, s2));
		verify(eventRepository).deleteById(1L);
	}

	@Test
	void getEventsPage_blankSortByAndNegativePageSize_shouldClampAndUseDefaultSort() {
		// Given
		Page<Event> page = new PageImpl<>(List.of(Event.builder().id(1L).build()));
		when(eventRepository.findAll(any(Pageable.class))).thenReturn(page);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

		// When
		Page<Event> result = eventService.getEventsPage(-5, 0, " ", "desc");

		// THEN
		assertThat(result.getContent()).hasSize(1);
		verify(eventRepository).findAll(captor.capture());
		Pageable used = captor.getValue();
		assertThat(used.getPageNumber()).isEqualTo(0);
		assertThat(used.getPageSize()).isEqualTo(1);
		assertThat(used.getSort().getOrderFor("id")).isNotNull();
		assertThat(used.getSort().getOrderFor("id").isDescending()).isTrue();
	}

	@Test
	void getEventById_found_shouldReturnEvent() {
		// Given
		Event event = Event.builder().id(1L).name("Test").category(EventCategory.MOVIE).basePrice(BigDecimal.TEN).build();
		when(eventRepository.findById(1L)).thenReturn(java.util.Optional.of(event));

		// When
		Event result = eventService.getEventById(1L);

		// Then
		assertThat(result).isSameAs(event);
		assertThat(result.getId()).isEqualTo(1L);
		verify(eventRepository).findById(1L);
	}

	@Test
	void getEventById_notFound_shouldThrowEventServiceException() {
		// Given
		when(eventRepository.findById(999L)).thenReturn(java.util.Optional.empty());

		// When
		Throwable thrown = catchThrowable(() -> eventService.getEventById(999L));

		// Then
		assertThat(thrown).isInstanceOf(EventServiceException.class)
				.hasMessage(EventConstants.ERROR_EVENT_NOT_FOUND);
	}

	@Test
	void getAllEvents_shouldReturnRepositoryResults() {
		// Given
		Event e1 = Event.builder().id(1L).name("A").build();
		Event e2 = Event.builder().id(2L).name("B").build();
		when(eventRepository.findAll()).thenReturn(List.of(e1, e2));

		// When
		List<Event> result = eventService.getAllEvents();

		// Then
		assertThat(result).containsExactly(e1, e2);
		verify(eventRepository).findAll();
	}

	@Test
	void getEventsPage_ascendingSort_shouldUseAscendingOrder() {
		// Given
		Page<Event> page = new PageImpl<>(List.of(Event.builder().id(1L).build()));
		when(eventRepository.findAll(any(Pageable.class))).thenReturn(page);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

		// When
		eventService.getEventsPage(0, 10, "name", "asc");

		// Then
		verify(eventRepository).findAll(captor.capture());
		Pageable used = captor.getValue();
		assertThat(used.getSort().getOrderFor("name")).isNotNull();
		assertThat(used.getSort().getOrderFor("name").isAscending()).isTrue();
	}

	@Test
	void getEventsPage_nullSortDir_shouldUseAscending() {
		// Given
		Page<Event> page = new PageImpl<>(List.of());
		when(eventRepository.findAll(any(Pageable.class))).thenReturn(page);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

		// When
		eventService.getEventsPage(0, 5, "id", null);

		// Then
		verify(eventRepository).findAll(captor.capture());
		assertThat(captor.getValue().getSort().getOrderFor("id").isAscending()).isTrue();
	}

	@Test
	void deleteEvent_responseHasBookingsNull_shouldNotThrowAndDelete() {
		// Given - response map with hasBookings null is treated as false (Boolean.TRUE.equals(null) = false)
		when(eventRepository.existsById(1L)).thenReturn(true);
		Show show = Show.builder().id(10L).startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).totalSeats(10).availableSeats(10).build();
		when(showRepository.findByEventId(1L)).thenReturn(List.of(show));
		Map<String, Boolean> responseWithNull = new HashMap<>();
		responseWithNull.put("hasBookings", null);
		when(bookingServiceClient.hasBookingsByShowId(10L)).thenReturn(responseWithNull);

		// When
		eventService.deleteEvent(1L);

		// Then
		verify(showRepository).deleteAll(any());
		verify(eventRepository).deleteById(1L);
	}
}

