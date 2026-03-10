package com.bookmyshow.event.service;

import com.bookmyshow.event.dto.EventRequest;
import com.bookmyshow.event.entity.Event;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service contract for Event CRUD. Admin only for add/delete.
 */
public interface EventService {

	/**
	 * Creates a new event (Admin only).
	 *
	 * @param request event payload
	 * @return created event
	 */
	Event createEvent(EventRequest request);

	/**
	 * Deletes an event by id (Admin only).
	 *
	 * @param eventId event id
	 */
	void deleteEvent(Long eventId);

	/**
	 * Finds event by id.
	 *
	 * @param eventId event id
	 * @return event
	 */
	Event getEventById(Long eventId);

	/**
	 * Lists all events.
	 *
	 * @return list of events
	 */
	List<Event> getAllEvents();

	/**
	 * Lists events in a paginated and sortable way for admin.
	 *
	 * @param page    page number (0-based)
	 * @param size    page size
	 * @param sortBy  property to sort by (e.g. name, id)
	 * @param sortDir sort direction (asc/desc)
	 * @return paginated events
	 */
	Page<Event> getEventsPage(int page, int size, String sortBy, String sortDir);
}
