package com.bookmyshow.frontend.controller;

import com.bookmyshow.frontend.client.BackendApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
public class BookingViewController {

	private final BackendApiClient apiClient;
	private final ObjectMapper objectMapper;

	@Value("${event.service.direct-url:}")
	private String eventServiceDirectUrl;

	@Value("${booking.service.direct-url:}")
	private String bookingServiceDirectUrl;

	public BookingViewController(BackendApiClient apiClient, ObjectMapper objectMapper) {
		this.apiClient = apiClient;
		this.objectMapper = objectMapper;
	}

	private boolean hasEventDirectUrl() {
		return eventServiceDirectUrl != null && !eventServiceDirectUrl.isBlank();
	}

	private boolean hasBookingDirectUrl() {
		return bookingServiceDirectUrl != null && !bookingServiceDirectUrl.isBlank();
	}

	/** API often returns id as Integer; path variable is Long. Compare as numbers. */
	private boolean bookingIdMatches(Long pathId, Object bookingId) {
		if (pathId == null || bookingId == null) return false;
		if (bookingId instanceof Number) return pathId.equals(((Number) bookingId).longValue());
		return pathId.equals(bookingId);
	}

	private List<?> fetchMyBookings() {
		try {
			var res = apiClient.get("/api/v1/bookings/my", List.class);
			return res.getBody() != null ? res.getBody() : List.of();
		} catch (Exception e) {
			if (hasBookingDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(bookingServiceDirectUrl + "/api/v1/bookings/my", List.class);
					return res.getBody() != null ? res.getBody() : List.of();
				} catch (Exception ex) { /* fall through */ }
			}
			return List.of();
		}
	}

	@GetMapping("/new")
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public String bookingPage(@RequestParam Long showId, Model model) {
		String path = "/api/v1/events/shows/" + showId;
		try {
			var showRes = apiClient.get(path, Map.class);
			model.addAttribute("show", showRes.getBody());
			model.addAttribute("showId", showId);
			return "booking";
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var showRes = apiClient.getAbsoluteUrl(eventServiceDirectUrl + path, Map.class);
					model.addAttribute("show", showRes.getBody());
					model.addAttribute("showId", showId);
					return "booking";
				} catch (Exception ex) { /* fall through */ }
			}
			model.addAttribute("error", "Show not found.");
			return "redirect:/browse";
		}
	}

	@PostMapping("/confirm")
	public String confirmBooking(@RequestParam Long showId,
			@RequestParam Integer numberOfSeats,
			@RequestParam(required = false) String couponCode,
			RedirectAttributes redirectAttributes) {
		Map<String, Object> body = new HashMap<>();
		body.put("showId", showId);
		body.put("numberOfSeats", numberOfSeats);
		body.put("couponCode", couponCode != null && !couponCode.isBlank() ? couponCode : null);
		try {
			var res = apiClient.post("/api/v1/bookings", body, Map.class);
			if (res.getBody() != null) {
				redirectAttributes.addFlashAttribute("booking", res.getBody());
				return "redirect:/bookings/confirmation?id=" + res.getBody().get("id");
			}
		} catch (Exception e) {
			Exception lastEx = e;
			if (hasBookingDirectUrl()) {
				try {
					var res = apiClient.postToAbsoluteUrl(bookingServiceDirectUrl + "/api/v1/bookings", body, Map.class);
					if (res != null && res.getBody() != null) {
						redirectAttributes.addFlashAttribute("booking", res.getBody());
						return "redirect:/bookings/confirmation?id=" + res.getBody().get("id");
					}
				} catch (Exception ex) {
					lastEx = ex;
				}
			}
			String msg = extractBookingErrorMessage(lastEx);
			redirectAttributes.addFlashAttribute("error", msg);
			return "redirect:/bookings/new?showId=" + showId;
		}
		redirectAttributes.addFlashAttribute("error", "Booking failed.");
		return "redirect:/bookings/new?showId=" + showId;
	}

	private String extractBookingErrorMessage(Exception e) {
		if (e == null) return "Booking failed.";
		if (e instanceof HttpClientErrorException h) {
			String bodyStr = h.getResponseBodyAsString();
			if (bodyStr != null && !bodyStr.isBlank()) {
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = objectMapper.readValue(bodyStr, Map.class);
					if (map != null) {
						Object message = map.get("message");
						if (message != null && !message.toString().isBlank()) return message.toString();
						Object error = map.get("error");
						if (error != null && !error.toString().isBlank()) return error.toString();
					}
				} catch (Exception ignored) {}
			}
		}
		return "Booking failed.";
	}

	@GetMapping("/confirmation")
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public String confirmation(@RequestParam Long id, Model model) {
		if (model.getAttribute("booking") != null) {
			return "booking-confirmation";
		}
		for (Object o : fetchMyBookings()) {
			Map<?, ?> b = (Map<?, ?>) o;
			if (id.equals(b.get("id"))) {
				model.addAttribute("booking", b);
				return "booking-confirmation";
			}
		}
		model.addAttribute("error", "Booking not found.");
		return "booking-confirmation";
	}

	@GetMapping("/my")
	public String myBookings(Model model) {
		try {
			var res = apiClient.get("/api/v1/bookings/my", List.class);
			model.addAttribute("bookings", res.getBody() != null ? res.getBody() : List.of());
		} catch (Exception e) {
			if (hasBookingDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(bookingServiceDirectUrl + "/api/v1/bookings/my", List.class);
					model.addAttribute("bookings", res.getBody() != null ? res.getBody() : List.of());
				} catch (Exception ex) {
					model.addAttribute("bookings", List.of());
					model.addAttribute("error", "Could not load bookings. Is Booking service running on 8084?");
				}
			} else {
				model.addAttribute("bookings", List.of());
				model.addAttribute("error", "Could not load bookings. Start Gateway and Booking service (8084).");
			}
		}
		model.addAttribute("activeTab", "all");
		return "my-bookings";
	}

	@GetMapping("/my/upcoming")
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public String myUpcoming(Model model) {
		try {
			var res = apiClient.get("/api/v1/bookings/my/upcoming", List.class);
			model.addAttribute("bookings", res.getBody() != null ? res.getBody() : List.of());
		} catch (Exception e) {
			if (hasBookingDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(bookingServiceDirectUrl + "/api/v1/bookings/my/upcoming", List.class);
					model.addAttribute("bookings", res.getBody() != null ? res.getBody() : List.of());
				} catch (Exception ex) {
					model.addAttribute("bookings", List.of());
					model.addAttribute("error", "Could not load upcoming bookings.");
				}
			} else {
				model.addAttribute("bookings", List.of());
				model.addAttribute("error", "Could not load upcoming bookings. Start Gateway and Booking service (8084).");
			}
		}
		model.addAttribute("activeTab", "upcoming");
		return "my-bookings";
	}

	@GetMapping("/my/past")
	public String myPast(Model model) {
		try {
			var res = apiClient.get("/api/v1/bookings/my/past", List.class);
			model.addAttribute("bookings", res.getBody() != null ? res.getBody() : List.of());
		} catch (Exception e) {
			if (hasBookingDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(bookingServiceDirectUrl + "/api/v1/bookings/my/past", List.class);
					model.addAttribute("bookings", res.getBody() != null ? res.getBody() : List.of());
				} catch (Exception ex) {
					model.addAttribute("bookings", List.of());
					model.addAttribute("error", "Could not load past bookings.");
				}
			} else {
				model.addAttribute("bookings", List.of());
				model.addAttribute("error", "Could not load past bookings. Start Gateway and Booking service (8084).");
			}
		}
		model.addAttribute("activeTab", "past");
		return "my-bookings";
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public String bookingDetail(@PathVariable Long id, Model model) {
		for (Object o : fetchMyBookings()) {
			Map<?, ?> b = (Map<?, ?>) o;
			if (bookingIdMatches(id, b.get("id"))) {
				model.addAttribute("booking", b);
				return "booking-detail";
			}
		}
		model.addAttribute("error", "Booking not found.");
		return "redirect:/bookings/my";
	}

	@GetMapping("/cancel/{id}")
	@PreAuthorize("isAuthenticated()")
	public String cancelPage(@PathVariable Long id, Model model) {
		List<?> list = fetchMyBookings();
		for (Object o : list) {
			Map<?, ?> b = (Map<?, ?>) o;
			if (bookingIdMatches(id, b.get("id"))) {
				model.addAttribute("booking", b);
				return "cancellation";
			}
		}
		model.addAttribute("error", "Booking not found. It may not belong to your account or the list could not be loaded.");
		return "cancellation";
	}

	@PostMapping("/cancel/{id}")
	@PreAuthorize("isAuthenticated()")
	public String confirmCancel(@PathVariable Long id,
			@RequestParam(required = false) Integer seatsToCancel,
			RedirectAttributes redirectAttributes) {
		String path = "/api/v1/bookings/" + id + "/cancel";
		if (seatsToCancel != null && seatsToCancel > 0) {
			path += "?seatsToCancel=" + seatsToCancel;
		}
		Exception lastException = null;
		if (hasBookingDirectUrl()) {
			try {
				String fullUrl = bookingServiceDirectUrl + path;
				var res = apiClient.postNoBodyAbsoluteUrl(fullUrl, Map.class);
				redirectAttributes.addFlashAttribute("cancelResponse", res.getBody());
				redirectAttributes.addFlashAttribute("success", "Cancellation completed.");
				return "redirect:/bookings/my";
			} catch (Exception e) {
				lastException = e;
			}
		}
		try {
			var res = apiClient.exchange(path, org.springframework.http.HttpMethod.POST, null, Map.class);
			redirectAttributes.addFlashAttribute("cancelResponse", res.getBody());
			redirectAttributes.addFlashAttribute("success", "Cancellation completed.");
			return "redirect:/bookings/my";
		} catch (Exception e) {
			lastException = e;
		}
		String msg = extractCancelErrorMessage(lastException);
		redirectAttributes.addFlashAttribute("error", msg);
		return "redirect:/bookings/cancel/" + id;
	}

	private String extractCancelErrorMessage(Exception e) {
		if (e == null) return "Cancellation failed.";
		if (e instanceof HttpClientErrorException h) {
			String bodyStr = h.getResponseBodyAsString();
			if (bodyStr != null && !bodyStr.isBlank()) {
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = objectMapper.readValue(bodyStr, Map.class);
					if (map != null) {
						Object message = map.get("message");
						if (message != null && !message.toString().isBlank()) return message.toString();
						Object error = map.get("error");
						if (error != null && !error.toString().isBlank()) return error.toString();
					}
				} catch (Exception ignored) {}
			}
			int status = h.getStatusCode().value();
			if (status == 401) return "Please log in again and try cancelling.";
			if (status == 403) return "You are not allowed to cancel this booking.";
		}
		return "Cancellation failed. Please try again or contact support.";
	}
}
