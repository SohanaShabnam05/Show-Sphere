package com.bookmyshow.event.controller;

import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.dto.ReleaseSeatsResponse;
import com.bookmyshow.event.dto.ShowDetailsDto;
import com.bookmyshow.event.dto.ShowRequest;
import com.bookmyshow.event.dto.ShowResponseDto;
import com.bookmyshow.event.dto.ShowSearchRequest;
import com.bookmyshow.event.entity.Show;
import com.bookmyshow.event.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bookmyshow.event.entity.EventCategory;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Show scheduling and search.
 */
@RestController
@RequestMapping(EventConstants.API_V1_EVENTS + EventConstants.SHOWS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shows", description = "Show scheduling and search APIs")
public class ShowController {

	private final ShowService showService;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Create show (Admin only). Use Authorize with Bearer token from admin login. Define eventId, theaterId, start/end time, totalSeats (max occupancy). Fails if same event at same venue at overlapping time.")
	public ResponseEntity<Show> createShow(@Valid @RequestBody ShowRequest request) {

		Show show = showService.createShow(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(show);
	}

	@GetMapping
	@Operation(summary = "Get all shows with full details (event name, category, base price, theater name, address, timings, seats)")
	public ResponseEntity<List<ShowResponseDto>> getAllShows() {

		List<ShowResponseDto> shows = showService.getAllShows();
		return ResponseEntity.ok(shows);
	}

	@GetMapping(EventConstants.ID_PATH)
	@Operation(summary = "Get one show by id with full details (event, theater, timings, seats)")
	public ResponseEntity<ShowResponseDto> getShowFullDetails(@PathVariable Long id) {

		ShowResponseDto show = showService.getShowFullDetails(id);
		return ResponseEntity.ok(show);
	}

	@GetMapping(EventConstants.ID_PATH + EventConstants.DETAILS)
	@Operation(summary = "Get show details for booking (event category, base price, start time, available seats)")
	public ResponseEntity<ShowDetailsDto> getShowDetails(@PathVariable Long id) {

		ShowDetailsDto details = showService.getShowDetails(id);
		return ResponseEntity.ok(details);
	}

	@PostMapping(EventConstants.ID_PATH + EventConstants.REPAIR)
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Repair show availability (Admin only). Clamps availableSeats to [0, totalSeats] if corrupted.")
	public ResponseEntity<Void> repairShowAvailability(@PathVariable Long id) {

		showService.repairShowAvailability(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping(EventConstants.ID_PATH + EventConstants.RESERVE)
	@Operation(summary = "Reserve seats (internal/Feign)")
	public ResponseEntity<Boolean> reserveSeats(@PathVariable Long id, @RequestParam("count") int count) {

		log.info("reserveSeats endpoint: id={}, count={}", id, count);
		boolean success = showService.reserveSeats(id, count);
		return ResponseEntity.ok(success);
	}

	@PostMapping(EventConstants.ID_PATH + EventConstants.RELEASE)
	@Operation(summary = "Release seats (internal/Feign). Only allows release <= reserved seats. Returns error if requested count exceeds reserved.")
	public ResponseEntity<ReleaseSeatsResponse> releaseSeats(@PathVariable Long id, @RequestParam("count") int count) {

		ReleaseSeatsResponse response = showService.releaseSeats(id, count);
		return ResponseEntity.ok(response);
	}

	@GetMapping(EventConstants.SEARCH)
	@Operation(summary = "Search shows by event type, event name, theater, date. Returns full details (event, theater, timings, seats) for each show.")
	public ResponseEntity<List<ShowResponseDto>> searchShows(
			@RequestParam(required = false) EventCategory category,
			@RequestParam(required = false) String eventName,
			@RequestParam(required = false) Long theaterId,
			@RequestParam(required = false) LocalDate showDate) {

		ShowSearchRequest searchRequest = ShowSearchRequest.builder()
				.category(category)
				.eventName(eventName)
				.theaterId(theaterId)
				.showDate(showDate)
				.build();
		List<ShowResponseDto> shows = showService.searchShows(searchRequest);
		return ResponseEntity.ok(shows);
	}
}
