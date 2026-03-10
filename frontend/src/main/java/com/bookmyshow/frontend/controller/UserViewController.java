package com.bookmyshow.frontend.controller;

import com.bookmyshow.frontend.client.BackendApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class UserViewController {

	private final BackendApiClient apiClient;

	@Value("${event.service.direct-url:}")
	private String eventServiceDirectUrl;

	public UserViewController(BackendApiClient apiClient) {
		this.apiClient = apiClient;
	}

	private boolean hasEventDirectUrl() {
		return eventServiceDirectUrl != null && !eventServiceDirectUrl.isBlank();
	}

	@GetMapping("/browse")
	public String browseEvents(Model model,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String eventName,
			@RequestParam(required = false) Long theaterId,
			@RequestParam(required = false) String showDate) {
		String path;
		if (category != null || eventName != null || theaterId != null || showDate != null) {
			StringBuilder q = new StringBuilder("/api/v1/events/shows/search?");
			if (category != null && !category.isBlank()) q.append("category=").append(category).append("&");
			if (eventName != null && !eventName.isBlank()) q.append("eventName=").append(java.net.URLEncoder.encode(eventName, java.nio.charset.StandardCharsets.UTF_8)).append("&");
			if (theaterId != null) q.append("theaterId=").append(theaterId).append("&");
			if (showDate != null && !showDate.isBlank()) q.append("showDate=").append(showDate).append("&");
			path = q.toString();
			if (path.endsWith("&")) path = path.substring(0, path.length() - 1);
		} else {
			path = "/api/v1/events/shows";
		}
		try {
			ResponseEntity<List> res = apiClient.get(path, List.class);
			model.addAttribute("shows", res.getBody() != null ? res.getBody() : List.of());
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					String fullPath = path.startsWith("http") ? path : eventServiceDirectUrl + path;
					ResponseEntity<List> res = apiClient.getAbsoluteUrl(fullPath, List.class);
					model.addAttribute("shows", res.getBody() != null ? res.getBody() : List.of());
				} catch (Exception ex) {
					model.addAttribute("shows", List.of());
					model.addAttribute("error", "Could not load shows. Is Event service running on 8083?");
				}
			} else {
				model.addAttribute("shows", List.of());
				model.addAttribute("error", "Could not load shows. Start Gateway and Event service (8083), or set event.service.direct-url.");
			}
		}
		model.addAttribute("events", fetchEvents());
		model.addAttribute("theaters", fetchTheaters());
		return "browse-events";
	}

	@GetMapping("/events/{showId}/details")
	public String eventDetails(@PathVariable Long showId, Model model) {
		String path = "/api/v1/events/shows/" + showId;
		try {
			ResponseEntity<Map> res = apiClient.get(path, Map.class);
			model.addAttribute("show", res.getBody());
			return "event-details";
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					ResponseEntity<Map> res = apiClient.getAbsoluteUrl(eventServiceDirectUrl + path, Map.class);
					model.addAttribute("show", res.getBody());
					return "event-details";
				} catch (Exception ex) { /* fall through */ }
			}
			model.addAttribute("error", "Show not found.");
			return "event-details";
		}
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> fetchEvents() {
		String path = "/api/v1/events";
		try {
			ResponseEntity<List> r = apiClient.get(path, List.class);
			return r.getBody() != null ? (List<Map<String, Object>>) r.getBody() : List.of();
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					ResponseEntity<List> r = apiClient.getAbsoluteUrl(eventServiceDirectUrl + path, List.class);
					return r.getBody() != null ? (List<Map<String, Object>>) r.getBody() : List.of();
				} catch (Exception ex) { /* fall through */ }
			}
			return List.of();
		}
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> fetchTheaters() {
		String path = "/api/v1/events/theaters";
		try {
			ResponseEntity<List> r = apiClient.get(path, List.class);
			return r.getBody() != null ? (List<Map<String, Object>>) r.getBody() : List.of();
		} catch (Exception e) {
			if (hasEventDirectUrl()) {
				try {
					ResponseEntity<List> r = apiClient.getAbsoluteUrl(eventServiceDirectUrl + path, List.class);
					return r.getBody() != null ? (List<Map<String, Object>>) r.getBody() : List.of();
				} catch (Exception ex) { /* fall through */ }
			}
			return List.of();
		}
	}
}
