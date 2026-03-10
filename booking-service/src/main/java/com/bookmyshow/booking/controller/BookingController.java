package com.bookmyshow.booking.controller;

import com.bookmyshow.booking.constant.BookingConstants;
import com.bookmyshow.booking.dto.BookingRequest;
import com.bookmyshow.booking.dto.CancelBookingResponse;
import com.bookmyshow.booking.entity.Booking;
import com.bookmyshow.booking.security.UserPrincipal;
import com.bookmyshow.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(BookingConstants.API_V1_BOOKINGS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookings", description = "Booking and cancellation APIs")
public class BookingController {

	private final BookingService bookingService;

	@PostMapping
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@Operation(summary = "Book tickets")
	public ResponseEntity<Booking> createBooking(
			Authentication authentication,
			@Valid @RequestBody BookingRequest request) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		Booking booking = bookingService.createBooking(principal.getUserId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(booking);
	}

	@GetMapping(BookingConstants.MY)
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@Operation(summary = "Get all my bookings (all statuses: upcoming, past, cancelled)")
	public ResponseEntity<List<Booking>> getMyBookings(Authentication authentication) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		List<Booking> bookings = bookingService.getMyBookings(principal.getUserId());
		return ResponseEntity.ok(bookings);
	}

	@GetMapping(BookingConstants.MY + BookingConstants.MY_UPCOMING)
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@Operation(summary = "Get my upcoming bookings (show start time in future)")
	public ResponseEntity<List<Booking>> getMyUpcomingBookings(Authentication authentication) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		List<Booking> bookings = bookingService.getMyBookings(principal.getUserId(), true);
		return ResponseEntity.ok(bookings);
	}

	@GetMapping(BookingConstants.MY + BookingConstants.MY_PAST)
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@Operation(summary = "Get my past bookings (show already started)")
	public ResponseEntity<List<Booking>> getMyPastBookings(Authentication authentication) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		List<Booking> bookings = bookingService.getMyBookings(principal.getUserId(), false);
		return ResponseEntity.ok(bookings);
	}

	@PostMapping(BookingConstants.ID_PATH + BookingConstants.CANCEL)
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@Operation(summary = "Full or partial cancellation. Returns refund amount and GST. Not allowed if event starts within 2 hours.")
	public ResponseEntity<CancelBookingResponse> cancelBooking(
			Authentication authentication,
			@PathVariable Long id,
			@RequestParam(required = false) Integer seatsToCancel) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		CancelBookingResponse response = bookingService.cancelBooking(principal.getUserId(), id, seatsToCancel);
		return ResponseEntity.ok(response);
	}

	@GetMapping(BookingConstants.HAS_BOOKINGS_BY_SHOW + "/{showId}")
	@Operation(summary = "Check if show has any bookings (internal/event-service)")
	public ResponseEntity<Map<String, Boolean>> hasBookingsByShowId(@PathVariable Long showId) {

		boolean exists = bookingService.hasBookingsByShowId(showId);
		return ResponseEntity.ok(Map.of("hasBookings", exists));
	}

	@GetMapping(value = BookingConstants.REPORTS + "/summary.csv", produces = "text/csv")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Download booking report CSV (Admin only). All booking details plus totals.")
	public ResponseEntity<String> getSummaryCsv(
			@Parameter(description = "Filter by event (omit for all events)")
			@RequestParam(required = false) Long eventId,
			@Parameter(description = "Filter from date (yyyy-MM-dd)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@Parameter(description = "Filter to date (yyyy-MM-dd)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		String csv = bookingService.getReportCsv(eventId, fromDate, toDate);
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=booking-summary.csv")
				.body(csv);
	}
}
