package com.bookmyshow.frontend.controller;

import com.bookmyshow.frontend.client.BackendApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminViewController {

	private final BackendApiClient apiClient;

	@Value("${event.service.direct-url:}")
	private String eventServiceDirectUrl;

	@Value("${coupon.service.direct-url:}")
	private String couponServiceDirectUrl;

	@Value("${booking.service.direct-url:}")
	private String bookingServiceDirectUrl;

	public AdminViewController(BackendApiClient apiClient) {
		this.apiClient = apiClient;
	}

	private boolean hasEventDirectUrl() {
		return eventServiceDirectUrl != null && !eventServiceDirectUrl.isBlank();
	}

	private boolean hasCouponDirectUrl() {
		return couponServiceDirectUrl != null && !couponServiceDirectUrl.isBlank();
	}

	private boolean hasBookingDirectUrl() {
		return bookingServiceDirectUrl != null && !bookingServiceDirectUrl.isBlank();
	}

	@GetMapping("/dashboard")
	public String dashboard() {
		return "admin-dashboard";
	}

	// ----- Create Event -----
	@GetMapping("/events/create")
	public String createEventPage(Model model) {
		model.addAttribute("eventRequest", new HashMap<String, Object>());
		return "create-event";
	}

	@PostMapping("/events/create")
	public String createEvent(@RequestParam String name, @RequestParam String category,
			@RequestParam String basePrice, RedirectAttributes redirectAttributes) {
		Map<String, Object> body = new HashMap<>();
		body.put("name", name);
		body.put("category", category);
		body.put("basePrice", basePrice);
		try {
			apiClient.post("/api/v1/events", body, Map.class);
			redirectAttributes.addFlashAttribute("success", "Event created successfully.");
			return "redirect:/admin/events";
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().value() == 404 && hasEventDirectUrl()) {
				try {
					apiClient.postToAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events", body, Map.class);
					redirectAttributes.addFlashAttribute("success", "Event created successfully.");
					return "redirect:/admin/events";
				} catch (Exception ignored) { /* fall through to error */ }
			}
			String msg = e.getStatusCode().value() == 404
					? "Event service not reachable. Start Discovery (8761), Gateway (8888), and Event service (8083). Or ensure Event service runs on 8083 and frontend has event.service.direct-url set."
					: parseMessage(e.getResponseBodyAsString());
			redirectAttributes.addFlashAttribute("error", msg);
			redirectAttributes.addFlashAttribute("name", name);
			redirectAttributes.addFlashAttribute("category", category);
			redirectAttributes.addFlashAttribute("basePrice", basePrice);
			return "redirect:/admin/events/create";
		} catch (org.springframework.web.client.ResourceAccessException e) {
			if (hasEventDirectUrl()) {
				try {
					apiClient.postToAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events", body, Map.class);
					redirectAttributes.addFlashAttribute("success", "Event created successfully.");
					return "redirect:/admin/events";
				} catch (Exception ignored) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", "Cannot reach backend. Start Gateway (8888) and Event service (8083). Frontend will try Event service directly if configured.");
			redirectAttributes.addFlashAttribute("name", name);
			redirectAttributes.addFlashAttribute("category", category);
			redirectAttributes.addFlashAttribute("basePrice", basePrice);
			return "redirect:/admin/events/create";
		}
	}

	// ----- Create Show -----
	@GetMapping("/shows/create")
	public String createShowPage(Model model) {
		model.addAttribute("events", fetchEvents());
		model.addAttribute("theaters", fetchTheaters());
		return "create-show";
	}

	@PostMapping("/shows/create")
	public String createShow(@RequestParam Long eventId, @RequestParam Long theaterId,
			@RequestParam String startTime, @RequestParam String endTime, @RequestParam Integer totalSeats,
			RedirectAttributes redirectAttributes) {
		Map<String, Object> body = new HashMap<>();
		body.put("eventId", eventId);
		body.put("theaterId", theaterId);
		body.put("startTime", startTime);
		body.put("endTime", endTime);
		body.put("totalSeats", totalSeats);
		try {
			apiClient.post("/api/v1/events/shows", body, Map.class);
			redirectAttributes.addFlashAttribute("success", "Show created successfully.");
			return "redirect:/admin/shows";
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().value() == 404 && hasEventDirectUrl()) {
				try {
					apiClient.postToAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/shows", body, Map.class);
					redirectAttributes.addFlashAttribute("success", "Show created successfully.");
					return "redirect:/admin/shows";
				} catch (Exception ignored) { /* fall through */ }
			}
			String errorMsg = messageForCreateShowError(e);
			redirectAttributes.addFlashAttribute("error", errorMsg);
			redirectAttributes.addFlashAttribute("eventId", eventId);
			redirectAttributes.addFlashAttribute("theaterId", theaterId);
			redirectAttributes.addFlashAttribute("startTime", startTime);
			redirectAttributes.addFlashAttribute("endTime", endTime);
			redirectAttributes.addFlashAttribute("totalSeats", totalSeats);
			return "redirect:/admin/shows/create";
		} catch (org.springframework.web.client.ResourceAccessException e) {
			if (hasEventDirectUrl()) {
				try {
					apiClient.postToAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/shows", body, Map.class);
					redirectAttributes.addFlashAttribute("success", "Show created successfully.");
					return "redirect:/admin/shows";
				} catch (Exception ignored) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", "Cannot reach Event service. Start Gateway and Event service (8083), or set event.service.direct-url.");
			redirectAttributes.addFlashAttribute("eventId", eventId);
			redirectAttributes.addFlashAttribute("theaterId", theaterId);
			redirectAttributes.addFlashAttribute("startTime", startTime);
			redirectAttributes.addFlashAttribute("endTime", endTime);
			redirectAttributes.addFlashAttribute("totalSeats", totalSeats);
			return "redirect:/admin/shows/create";
		}
	}

	// ----- Create Coupon -----
	@GetMapping("/coupons/create")
	public String createCouponPage(Model model) {
		return "create-coupon";
	}

	@PostMapping("/coupons/create")
	public String createCoupon(@RequestParam String code, @RequestParam String discountType,
			@RequestParam String discountValue, @RequestParam String validFrom, @RequestParam String validTo,
			@RequestParam(required = false) String eventCategory,
			@RequestParam(required = false) Integer maxRedemptions,
			RedirectAttributes redirectAttributes) {
		Map<String, Object> body = new HashMap<>();
		body.put("code", code);
		body.put("discountType", discountType);
		body.put("discountValue", discountValue);
		body.put("validFrom", validFrom);
		body.put("validTo", validTo);
		if (eventCategory != null && !eventCategory.isBlank()) body.put("eventCategory", eventCategory);
		if (maxRedemptions != null) body.put("maxRedemptions", maxRedemptions);
		try {
			apiClient.post("/api/v1/coupons", body, Map.class);
			redirectAttributes.addFlashAttribute("success", "Coupon created successfully.");
			return "redirect:/admin/coupons";
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().value() == 404 && hasCouponDirectUrl()) {
				try {
					apiClient.postToAbsoluteUrl(couponServiceDirectUrl + "/api/v1/coupons", body, Map.class);
					redirectAttributes.addFlashAttribute("success", "Coupon created successfully.");
					return "redirect:/admin/coupons";
				} catch (Exception ignored) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", parseMessage(e.getResponseBodyAsString()));
			redirectAttributes.addFlashAttribute("code", code);
			redirectAttributes.addFlashAttribute("discountType", discountType);
			redirectAttributes.addFlashAttribute("discountValue", discountValue);
			redirectAttributes.addFlashAttribute("validFrom", validFrom);
			redirectAttributes.addFlashAttribute("validTo", validTo);
			redirectAttributes.addFlashAttribute("eventCategory", eventCategory);
			redirectAttributes.addFlashAttribute("maxRedemptions", maxRedemptions);
			return "redirect:/admin/coupons/create";
		} catch (org.springframework.web.client.ResourceAccessException e) {
			if (hasCouponDirectUrl()) {
				try {
					apiClient.postToAbsoluteUrl(couponServiceDirectUrl + "/api/v1/coupons", body, Map.class);
					redirectAttributes.addFlashAttribute("success", "Coupon created successfully.");
					return "redirect:/admin/coupons";
				} catch (Exception ignored) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", "Cannot reach Coupon service. Start Gateway and Coupon service (8085), or set coupon.service.direct-url.");
			redirectAttributes.addFlashAttribute("code", code);
			redirectAttributes.addFlashAttribute("discountType", discountType);
			redirectAttributes.addFlashAttribute("discountValue", discountValue);
			redirectAttributes.addFlashAttribute("validFrom", validFrom);
			redirectAttributes.addFlashAttribute("validTo", validTo);
			redirectAttributes.addFlashAttribute("eventCategory", eventCategory);
			redirectAttributes.addFlashAttribute("maxRedemptions", maxRedemptions);
			return "redirect:/admin/coupons/create";
		}
	}

	// ----- Manage Events -----
	@GetMapping("/events")
	public String manageEvents(Model model,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {
		String path = "/api/v1/events/admin?page=" + page + "&size=" + size;
		if (sortBy != null && !sortBy.isBlank()) path += "&sortBy=" + sortBy + "&sortDir=" + sortDir;
		try {
			var res = apiClient.get(path, Map.class);
			model.addAttribute("eventsPage", res.getBody());
			model.addAttribute("currentPage", page);
			model.addAttribute("size", size);
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(eventServiceDirectUrl + path, Map.class);
					model.addAttribute("eventsPage", res.getBody());
					model.addAttribute("currentPage", page);
					model.addAttribute("size", size);
				} catch (Exception ex) {
					model.addAttribute("eventsPage", Map.of("content", List.of(), "totalPages", 0));
				}
			} else {
				model.addAttribute("eventsPage", Map.of("content", List.of(), "totalPages", 0));
			}
		}
		return "manage-events";
	}

	@PostMapping("/events/delete/{id}")
	public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			apiClient.delete("/api/v1/events/" + id);
			redirectAttributes.addFlashAttribute("success", "Event deleted.");
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					apiClient.deleteAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/" + id);
					redirectAttributes.addFlashAttribute("success", "Event deleted.");
				} catch (Exception ex) {
					redirectAttributes.addFlashAttribute("error", parseMessage(ex instanceof HttpClientErrorException h ? h.getResponseBodyAsString() : null));
				}
			} else {
				redirectAttributes.addFlashAttribute("error", parseMessage(e instanceof HttpClientErrorException h ? h.getResponseBodyAsString() : null));
			}
		}
		return "redirect:/admin/events";
	}

	@GetMapping("/events/view/{id}")
	public String viewEvent(@PathVariable Long id, Model model) {
		try {
			var res = apiClient.get("/api/v1/events/" + id, Map.class);
			model.addAttribute("event", res.getBody());
			return "admin-event-view";
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/" + id, Map.class);
					model.addAttribute("event", res.getBody());
					return "admin-event-view";
				} catch (Exception ex) { /* fall through */ }
			}
			model.addAttribute("error", "Could not load event.");
			return "redirect:/admin/events";
		}
	}

	// ----- Manage Shows (list + view) -----
	@GetMapping("/shows")
	public String manageShows(Model model,
			@RequestParam(required = false) String eventName,
			@RequestParam(required = false) String category) {
		String path;
		if ((eventName != null && !eventName.isBlank()) || (category != null && !category.isBlank())) {
			path = "/api/v1/events/shows/search?";
			if (eventName != null && !eventName.isBlank()) path += "eventName=" + URLEncoder.encode(eventName.trim(), StandardCharsets.UTF_8) + "&";
			if (category != null && !category.isBlank()) path += "category=" + category + "&";
			path = path.replaceFirst("&$", "");
		} else {
			path = "/api/v1/events/shows";
		}
		try {
			var res = apiClient.get(path, List.class);
			model.addAttribute("shows", res.getBody() != null ? res.getBody() : List.of());
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					String fullUrl = path.startsWith("http") ? path : eventServiceDirectUrl + path;
					var res = apiClient.getAbsoluteUrl(fullUrl, List.class);
					model.addAttribute("shows", res.getBody() != null ? res.getBody() : List.of());
				} catch (Exception ex) {
					model.addAttribute("shows", List.of());
					model.addAttribute("error", "Could not load shows. Is Event service running on 8083?");
				}
			} else {
				model.addAttribute("shows", List.of());
				model.addAttribute("error", "Could not load shows. Start Gateway + Event service, or set event.service.direct-url=http://localhost:8083");
			}
		}
		model.addAttribute("eventName", eventName != null ? eventName : "");
		model.addAttribute("category", category != null ? category : "");
		return "manage-shows";
	}

	@GetMapping("/shows/view/{id}")
	public String viewShow(@PathVariable Long id, Model model) {
		try {
			var res = apiClient.get("/api/v1/events/shows/" + id, Map.class);
			model.addAttribute("show", res.getBody());
			return "admin-show-view";
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/shows/" + id, Map.class);
					model.addAttribute("show", res.getBody());
					return "admin-show-view";
				} catch (Exception ex) { /* fall through */ }
			}
			model.addAttribute("error", "Could not load show.");
			return "redirect:/admin/shows";
		}
	}

	// ----- Edit Show (reserve / release / repair) -----
	@GetMapping("/shows/edit/{id}")
	public String editShowPage(@PathVariable Long id, Model model) {
		try {
			var res = apiClient.get("/api/v1/events/shows/" + id, Map.class);
			model.addAttribute("show", res.getBody());
			return "edit-show";
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/shows/" + id, Map.class);
					model.addAttribute("show", res.getBody());
					return "edit-show";
				} catch (Exception ex) { /* fall through */ }
			}
			model.addAttribute("error", "Could not load show.");
			return "redirect:/admin/shows";
		}
	}

	@PostMapping("/shows/edit/{id}/reserve")
	public String reserveSeats(@PathVariable Long id, @RequestParam int count, RedirectAttributes redirectAttributes) {
		String path = "/api/v1/events/shows/" + id + "/reserve?count=" + count;
		try {
			apiClient.postNoBody(path, Boolean.class);
			redirectAttributes.addFlashAttribute("success", "Reserved " + count + " seat(s).");
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					apiClient.postNoBodyAbsoluteUrl(eventServiceDirectUrl + path, Boolean.class);
					redirectAttributes.addFlashAttribute("success", "Reserved " + count + " seat(s).");
					return "redirect:/admin/shows/edit/" + id;
				} catch (Exception ex) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", parseMessage(e instanceof HttpClientErrorException h ? h.getResponseBodyAsString() : null));
		}
		return "redirect:/admin/shows/edit/" + id;
	}

	@PostMapping("/shows/edit/{id}/release")
	public String releaseSeats(@PathVariable Long id, @RequestParam int count, RedirectAttributes redirectAttributes) {
		String path = "/api/v1/events/shows/" + id + "/release?count=" + count;
		try {
			var res = apiClient.postNoBody(path, Map.class);
			Object body = res.getBody();
			String msg = body != null && body instanceof Map m && m.containsKey("message") ? String.valueOf(m.get("message")) : (count + " seat(s) released.");
			redirectAttributes.addFlashAttribute("success", msg);
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var res = apiClient.postNoBodyAbsoluteUrl(eventServiceDirectUrl + path, Map.class);
					Object body = res != null && res.getBody() != null ? res.getBody() : null;
					String msg = body instanceof Map m && m.containsKey("message") ? String.valueOf(m.get("message")) : (count + " seat(s) released.");
					redirectAttributes.addFlashAttribute("success", msg);
					return "redirect:/admin/shows/edit/" + id;
				} catch (Exception ex) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", parseMessage(e instanceof HttpClientErrorException h ? h.getResponseBodyAsString() : null));
		}
		return "redirect:/admin/shows/edit/" + id;
	}

	@PostMapping("/shows/edit/{id}/repair")
	public String repairShow(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		String path = "/api/v1/events/shows/" + id + "/repair";
		try {
			apiClient.postNoBody(path, Void.class);
			redirectAttributes.addFlashAttribute("success", "Show availability repaired.");
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					apiClient.postNoBodyAbsoluteUrl(eventServiceDirectUrl + path, Void.class);
					redirectAttributes.addFlashAttribute("success", "Show availability repaired.");
					return "redirect:/admin/shows/edit/" + id;
				} catch (Exception ex) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", parseMessage(e instanceof HttpClientErrorException h ? h.getResponseBodyAsString() : null));
		}
		return "redirect:/admin/shows/edit/" + id;
	}

	// ----- Manage Coupons (list + view) -----
	@GetMapping("/coupons")
	public String manageCoupons(Model model) {
		String path = "/api/v1/coupons";
		try {
			var res = apiClient.get(path, List.class);
			model.addAttribute("coupons", res.getBody() != null ? res.getBody() : List.of());
		} catch (Exception e) {
			if (hasCouponDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(couponServiceDirectUrl + path, List.class);
					model.addAttribute("coupons", res.getBody() != null ? res.getBody() : List.of());
				} catch (Exception ex) {
					model.addAttribute("coupons", List.of());
					model.addAttribute("error", "Could not load coupons. Is Coupon service running on 8085?");
				}
			} else {
				model.addAttribute("coupons", List.of());
				model.addAttribute("error", "Could not load coupons. Start Gateway and Coupon service (8085), or set coupon.service.direct-url.");
			}
		}
		model.addAttribute("today", java.time.LocalDate.now().toString());
		return "manage-coupons";
	}

	@GetMapping("/coupons/view/{id}")
	public String viewCoupon(@PathVariable Long id, Model model) {
		String path = "/api/v1/coupons/" + id;
		try {
			var res = apiClient.get(path, Map.class);
			model.addAttribute("coupon", res.getBody());
			model.addAttribute("today", java.time.LocalDate.now().toString());
			return "admin-coupon-view";
		} catch (Exception e) {
			if (hasCouponDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(couponServiceDirectUrl + path, Map.class);
					model.addAttribute("coupon", res.getBody());
					model.addAttribute("today", java.time.LocalDate.now().toString());
					return "admin-coupon-view";
				} catch (Exception ex) { /* fall through */ }
			}
			model.addAttribute("error", "Could not load coupon.");
			return "redirect:/admin/coupons";
		}
	}

	// ----- Theaters (create and view only) -----
	@GetMapping("/theaters")
	public String manageTheaters(Model model) {
		model.addAttribute("theaters", fetchTheaters());
		return "manage-theaters";
	}

	@GetMapping("/theaters/create")
	public String createTheaterPage() {
		return "create-theater";
	}

	@PostMapping("/theaters/create")
	public String createTheater(@RequestParam String name, @RequestParam(required = false) String address,
			RedirectAttributes redirectAttributes) {
		Map<String, Object> body = new HashMap<>();
		body.put("name", name);
		if (address != null && !address.isBlank()) body.put("address", address);
		try {
			apiClient.post("/api/v1/events/theaters", body, Map.class);
			redirectAttributes.addFlashAttribute("success", "Theater created successfully.");
			return "redirect:/admin/theaters";
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().value() == 404 && hasEventDirectUrl()) {
				try {
					apiClient.postToAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/theaters", body, Map.class);
					redirectAttributes.addFlashAttribute("success", "Theater created successfully.");
					return "redirect:/admin/theaters";
				} catch (Exception ignored) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", parseMessage(e.getResponseBodyAsString()));
			redirectAttributes.addFlashAttribute("name", name);
			redirectAttributes.addFlashAttribute("address", address);
			return "redirect:/admin/theaters/create";
		} catch (org.springframework.web.client.ResourceAccessException e) {
			if (hasEventDirectUrl()) {
				try {
					apiClient.postToAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/theaters", body, Map.class);
					redirectAttributes.addFlashAttribute("success", "Theater created successfully.");
					return "redirect:/admin/theaters";
				} catch (Exception ignored) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", "Cannot reach Event service. Start Gateway and Event service (8083), or set event.service.direct-url.");
			redirectAttributes.addFlashAttribute("name", name);
			redirectAttributes.addFlashAttribute("address", address);
			return "redirect:/admin/theaters/create";
		}
	}

	@GetMapping("/theaters/view/{id}")
	public String viewTheater(@PathVariable Long id, Model model) {
		try {
			var res = apiClient.get("/api/v1/events/theaters/" + id, Map.class);
			model.addAttribute("theater", res.getBody());
			return "admin-theater-view";
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var res = apiClient.getAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/theaters/" + id, Map.class);
					model.addAttribute("theater", res.getBody());
					return "admin-theater-view";
				} catch (Exception ex) { /* fall through */ }
			}
			model.addAttribute("error", "Could not load theater.");
			return "redirect:/admin/theaters";
		}
	}

	// ----- Reports -----
	@GetMapping("/reports")
	public String reports(Model model,
			@RequestParam(required = false) Long eventId,
			@RequestParam(required = false) String fromDate,
			@RequestParam(required = false) String toDate) {
		model.addAttribute("events", fetchEvents());
		model.addAttribute("eventId", eventId);
		model.addAttribute("fromDate", fromDate);
		model.addAttribute("toDate", toDate);
		String path = "/api/v1/bookings/reports/summary.csv";
		if (eventId != null || (fromDate != null && !fromDate.isBlank()) || (toDate != null && !toDate.isBlank())) {
			path += "?";
			if (eventId != null) path += "eventId=" + eventId + "&";
			if (fromDate != null && !fromDate.isBlank()) path += "fromDate=" + fromDate + "&";
			if (toDate != null && !toDate.isBlank()) path += "toDate=" + toDate + "&";
		}
		try {
			String csv = apiClient.getCsv(path);
			model.addAttribute("csvContent", csv != null ? csv : "");
			model.addAttribute("reportRows", parseCsvToRows(csv));
		} catch (Exception e) {
			if (hasBookingDirectUrl()) {
				try {
					String fullUrl = bookingServiceDirectUrl + path;
					String csv = apiClient.getCsvAbsoluteUrl(fullUrl);
					model.addAttribute("csvContent", csv != null ? csv : "");
					model.addAttribute("reportRows", parseCsvToRows(csv));
				} catch (Exception ex) {
					model.addAttribute("csvContent", "");
					model.addAttribute("reportRows", List.of());
					model.addAttribute("reportError", "Could not load report. Is Booking service running on 8084?");
				}
			} else {
				model.addAttribute("csvContent", "");
				model.addAttribute("reportRows", List.of());
				model.addAttribute("reportError", "Could not load report. Start Gateway and Booking service (8084), or set booking.service.direct-url.");
			}
		}
		return "reports";
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> fetchEvents() {
		try {
			var r = apiClient.get("/api/v1/events", List.class);
			return r.getBody() != null ? (List<Map<String, Object>>) r.getBody() : List.of();
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var r = apiClient.getAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events", List.class);
					return r.getBody() != null ? (List<Map<String, Object>>) r.getBody() : List.of();
				} catch (Exception ex) { /* fall through */ }
			}
			return List.of();
		}
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> fetchTheaters() {
		try {
			var r = apiClient.get("/api/v1/events/theaters", List.class);
			return r.getBody() != null ? (List<Map<String, Object>>) r.getBody() : List.of();
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					var r = apiClient.getAbsoluteUrl(eventServiceDirectUrl + "/api/v1/events/theaters", List.class);
					return r.getBody() != null ? (List<Map<String, Object>>) r.getBody() : List.of();
				} catch (Exception ex) { /* fall through */ }
			}
			return List.of();
		}
	}

	/**
	 * For create-show API errors: show a friendly "show time is overlapping" message
	 * when the backend returns overlap (400) or when Gateway returns 404 for this path.
	 */
	private String messageForCreateShowError(HttpClientErrorException e) {
		int status = e.getStatusCode().value();
		String body = e.getResponseBodyAsString();
		if (status == 404) {
			return "The show time is overlapping.";
		}
		if (body != null && (body.toLowerCase().contains("overlap"))) {
			return "The show time is overlapping.";
		}
		return parseMessage(body);
	}

	private String parseMessage(String body) {
		if (body == null) return "Request failed.";
		if (body.contains("message")) {
			try {
				int i = body.indexOf("\"message\"");
				if (i >= 0) {
					int start = body.indexOf("\"", i + 10) + 1;
					int end = body.indexOf("\"", start);
					if (end > start) return body.substring(start, end);
				}
			} catch (Exception ignored) {}
		}
		return body.length() > 200 ? body.substring(0, 200) : body;
	}

	private List<List<String>> parseCsvToRows(String csv) {
		if (csv == null || csv.isBlank()) return List.of();
		List<List<String>> rows = new java.util.ArrayList<>();
		String[] lines = csv.split("\n");
		for (String line : lines) {
			List<String> cells = new java.util.ArrayList<>();
			boolean inQuotes = false;
			StringBuilder cell = new StringBuilder();
			for (int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				if (c == '"') inQuotes = !inQuotes;
				else if ((c == ',' && !inQuotes) || c == '\r') {
					cells.add(cell.toString().trim());
					cell = new StringBuilder();
				} else if (c != '\r') cell.append(c);
			}
			cells.add(cell.toString().trim());
			rows.add(cells);
		}
		return rows;
	}
}
