package com.bookmyshow.event.controller;

import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.dto.EventRequest;
import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Event CRUD. Add/Delete restricted to Admin via gateway.
 */
@RestController
@RequestMapping(EventConstants.API_V1_EVENTS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Events", description = "Event management APIs")
public class EventController {

	private final EventService eventService;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Create event (Admin only). Define name, category (MOVIE/CONCERT/LIVE_SHOW), base price per seat.")
	public ResponseEntity<Event> createEvent(@Valid @RequestBody EventRequest request) {

		Event event = eventService.createEvent(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(event);
	}

	@DeleteMapping(EventConstants.ID_PATH)
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Delete event (Admin only). Fails if event has associated bookings.")
	public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {

		eventService.deleteEvent(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping(EventConstants.ID_PATH)
	@Operation(summary = "Get event by id")
	public ResponseEntity<Event> getEventById(@PathVariable Long id) {

		Event event = eventService.getEventById(id);
		return ResponseEntity.ok(event);
	}

	@GetMapping
	@Operation(summary = "List all events")
	public ResponseEntity<List<Event>> getAllEvents() {

		List<Event> events = eventService.getAllEvents();
		return ResponseEntity.ok(events);
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Paginated events list for admin (sortable by id or name)")
	public ResponseEntity<Page<Event>> getEventsPage(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {

		Page<Event> eventsPage = eventService.getEventsPage(page, size, sortBy, sortDir);
		return ResponseEntity.ok(eventsPage);
	}
}
