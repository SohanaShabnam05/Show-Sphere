package com.bookmyshow.event.service.impl;

import com.bookmyshow.event.client.BookingServiceClient;
import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.dto.EventRequest;
import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.entity.Show;
import com.bookmyshow.event.exception.EventServiceException;
import com.bookmyshow.event.repository.EventRepository;
import com.bookmyshow.event.repository.ShowRepository;
import com.bookmyshow.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link EventService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

	private final EventRepository eventRepository;
	private final ShowRepository showRepository;
	private final BookingServiceClient bookingServiceClient;

	@Override
	@Transactional
	@CacheEvict(value = { "eventsBrowse", "eventsBrowsePaged" }, allEntries = true)
	public Event createEvent(EventRequest request) {

		log.debug("Creating event: {}", request.getName());

		Event event = Event.builder()
				.name(request.getName())
				.category(request.getCategory())
				.basePrice(request.getBasePrice())
				.build();
		return eventRepository.save(event);
	}

	@Override
	@Transactional
	@CacheEvict(value = { "eventsBrowse", "eventsBrowsePaged" }, allEntries = true)
	public void deleteEvent(Long eventId) {

		log.debug("Deleting event id: {}", eventId);

		if (!eventRepository.existsById(eventId)) {
			throw new EventServiceException(EventConstants.ERROR_EVENT_NOT_FOUND);
		}
		List<Show> shows = showRepository.findByEventId(eventId);
		for (Show show : shows) {
			Map<String, Boolean> response = bookingServiceClient.hasBookingsByShowId(show.getId());
			if (Boolean.TRUE.equals(response.get("hasBookings"))) {
				throw new EventServiceException(EventConstants.ERROR_EVENT_HAS_BOOKINGS);
			}
		}
		showRepository.deleteAll(shows);
		eventRepository.deleteById(eventId);
	}

	@Override
	@Cacheable("eventsBrowse")
	public Event getEventById(Long eventId) {

		return eventRepository.findById(eventId)
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_EVENT_NOT_FOUND));
	}

	@Override
	@Cacheable("eventsBrowse")
	public List<Event> getAllEvents() {

		return eventRepository.findAll();
	}

	@Override
	@Cacheable("eventsBrowsePaged")
	public Page<Event> getEventsPage(int page, int size, String sortBy, String sortDir) {

		String sortProperty = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
		Sort sort = Sort.by(sortProperty);
		if ("desc".equalsIgnoreCase(sortDir)) {
			sort = sort.descending();
		} else {
			sort = sort.ascending();
		}
		PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);
		return eventRepository.findAll(pageable);
	}
}
