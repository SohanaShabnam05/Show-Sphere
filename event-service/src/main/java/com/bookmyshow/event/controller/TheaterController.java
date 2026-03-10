package com.bookmyshow.event.controller;

import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.dto.TheaterRequest;
import com.bookmyshow.event.entity.Theater;
import com.bookmyshow.event.service.TheaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Theater APIs. Create (Admin only), list, get by id.
 */
@RestController
@RequestMapping(EventConstants.API_V1_EVENTS + EventConstants.THEATERS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Theaters", description = "Theater APIs")
public class TheaterController {

	private final TheaterService theaterService;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Create theater (Admin only). Use the returned id as theaterId when creating a show.")
	public ResponseEntity<Theater> createTheater(@Valid @RequestBody TheaterRequest request) {

		Theater theater = theaterService.createTheater(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(theater);
	}

	@GetMapping
	@Operation(summary = "List all theaters")
	public ResponseEntity<List<Theater>> getAllTheaters() {

		List<Theater> theaters = theaterService.getAllTheaters();
		return ResponseEntity.ok(theaters);
	}

	@GetMapping(EventConstants.ID_PATH)
	@Operation(summary = "Get theater by id")
	public ResponseEntity<Theater> getTheaterById(@PathVariable Long id) {

		Theater theater = theaterService.getTheaterById(id);
		return ResponseEntity.ok(theater);
	}
}
