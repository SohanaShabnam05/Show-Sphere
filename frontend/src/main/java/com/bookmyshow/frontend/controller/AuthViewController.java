package com.bookmyshow.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bookmyshow.frontend.client.BackendApiClient;
import com.bookmyshow.frontend.dto.JwtResponse;
import com.bookmyshow.frontend.dto.LoginRequest;
import com.bookmyshow.frontend.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping
public class AuthViewController {

	private static final Logger log = LoggerFactory.getLogger(AuthViewController.class);
	private static final String AUTH_ERROR_KEY = "message";

	private final BackendApiClient apiClient;
	private final ObjectMapper objectMapper;

	@Value("${backend.api.base-url:http://localhost:8888}")
	private String gatewayBaseUrl;

	@Value("${jwt.cookie-name:BMS_TOKEN}")
	private String jwtCookieName;

	@Value("${jwt.cookie-max-age:86400}")
	private int cookieMaxAgeSeconds;

	/** When set, register/login retry directly to auth-service if gateway returns 404 or is unreachable. */
	@Value("${auth.service.direct-url:}")
	private String authServiceDirectUrl;

	public AuthViewController(BackendApiClient apiClient, ObjectMapper objectMapper) {
		this.apiClient = apiClient;
		this.objectMapper = objectMapper;
	}

	/**
	 * True if the response indicates under-age (auth-service: "Only users 18 years or older are allowed to sign up.").
	 * Ensures we show an age message instead of "Auth service not reachable" when user is under 18.
	 */
	private boolean isUnderAgeError(String msg, String body) {
		String combined = (msg != null ? msg : "") + " " + (body != null ? body : "");
		String lower = combined.toLowerCase();
		return lower.contains("18") && (lower.contains("year") || lower.contains("age") || lower.contains("older") || lower.contains("sign up"));
	}

	/**
	 * Extract error message from backend JSON. Auth-service returns {"message": "..."}.
	 * Also tries "error", "msg"; if body is not JSON, tries simple pattern for "message":"value".
	 */
	private String extractAuthErrorMessage(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) return null;
		String trimmed = responseBody.trim();
		// Try JSON parse first (auth-service and most backends use "message")
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = objectMapper.readValue(trimmed, Map.class);
			if (map != null) {
				for (String key : new String[] { AUTH_ERROR_KEY, "error", "msg" }) {
					Object val = map.get(key);
					if (val != null) {
						String s = val.toString().trim();
						if (!s.isEmpty()) return s;
					}
				}
			}
		} catch (Exception e) {
			log.trace("Could not parse auth error as JSON: {}", e.getMessage());
		}
		// Fallback: look for "message":"..." or "message": "..." in raw body
		if (trimmed.startsWith("{")) {
			int idx = trimmed.indexOf("\"message\"");
			if (idx >= 0) {
				int valueStart = trimmed.indexOf("\"", idx + 10) + 1;
				int valueEnd = trimmed.indexOf("\"", valueStart);
				if (valueEnd > valueStart) {
					return trimmed.substring(valueStart, valueEnd).replace("\\\"", "\"").trim();
				}
			}
		}
		return null;
	}

	@GetMapping("/login")
	public String loginPage(Model model) {
		model.addAttribute("loginRequest", new LoginRequest());
		return "login";
	}

	@PostMapping("/login")
	public String login(@ModelAttribute LoginRequest request, HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		try {
			ResponseEntity<JwtResponse> res = apiClient.post("/api/v1/auth/login", request, JwtResponse.class);
			if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
				return setJwtAndRedirect(response, res.getBody(), redirectAttributes);
			}
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().value() == 404 && hasDirectAuthUrl()) {
				try {
					ResponseEntity<JwtResponse> res = apiClient.postToAbsoluteUrl(authServiceDirectUrl + "/api/v1/auth/login", request, JwtResponse.class);
					if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
						return setJwtAndRedirect(response, res.getBody(), redirectAttributes);
					}
				} catch (Exception ignored) { /* fall through to error */ }
			}
			String msg = extractAuthErrorMessage(e.getResponseBodyAsString());
			if (e.getStatusCode().value() == 404 || (msg != null && msg.trim().equalsIgnoreCase("Not Found"))) {
				msg = "Auth service not reachable. Start: Discovery (8761), Auth (8081), then API Gateway (8888). Use this app at http://localhost:9090.";
			} else if (msg == null || msg.isBlank()) msg = "Invalid email or password.";
			redirectAttributes.addFlashAttribute("error", msg);
			return "redirect:/login";
		} catch (ResourceAccessException e) {
			if (hasDirectAuthUrl()) {
				try {
					ResponseEntity<JwtResponse> res = apiClient.postToAbsoluteUrl(authServiceDirectUrl + "/api/v1/auth/login", request, JwtResponse.class);
					if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
						return setJwtAndRedirect(response, res.getBody(), redirectAttributes);
					}
				} catch (Exception ignored) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", "Cannot reach server. Is the backend running? Start Auth (8081) or Gateway (8888).");
			return "redirect:/login";
		}
		redirectAttributes.addFlashAttribute("error", "Login failed.");
		return "redirect:/login";
	}

	private boolean hasDirectAuthUrl() {
		return authServiceDirectUrl != null && !authServiceDirectUrl.isBlank();
	}

	private String setJwtAndRedirect(HttpServletResponse response, JwtResponse jwt, RedirectAttributes redirectAttributes) {
		Cookie cookie = new Cookie(jwtCookieName, jwt.getToken());
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setMaxAge(cookieMaxAgeSeconds);
		cookie.setSecure(false);
		response.addCookie(cookie);
		String role = jwt.getRole() != null ? jwt.getRole().toUpperCase() : "";
		if (role.contains("ADMIN")) return "redirect:/admin/dashboard";
		return "redirect:/home";
	}

	@GetMapping("/register")
	public String registerPage(Model model) {
		model.addAttribute("registerRequest", new RegisterRequest());
		return "register";
	}

	@PostMapping("/register")
	public String register(@ModelAttribute RegisterRequest request, HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		// Build JSON body exactly as auth-service expects (name, email, password, mobile, address, dateOfBirth ISO date)
		Map<String, Object> registerBody = new HashMap<>();
		registerBody.put("name", request.getName() != null ? request.getName().trim() : "");
		registerBody.put("email", request.getEmail() != null ? request.getEmail().trim() : "");
		registerBody.put("password", request.getPassword() != null ? request.getPassword() : "");
		registerBody.put("mobile", request.getMobile() != null ? request.getMobile().trim() : "");
		registerBody.put("address", request.getAddress() != null ? request.getAddress().trim() : null);
		registerBody.put("dateOfBirth", request.getDateOfBirth() != null ? request.getDateOfBirth().toString() : null);
		String registerPath = "/api/v1/auth/register";
		log.info("Register: calling auth-service via gateway at {}{}", gatewayBaseUrl, registerPath);
		try {
			ResponseEntity<JwtResponse> res = apiClient.post(registerPath, registerBody, JwtResponse.class);
			if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
				redirectAttributes.addFlashAttribute("success", "Registration successful. You are now logged in.");
				return setJwtAndRedirect(response, res.getBody(), redirectAttributes);
			}
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().value() == 404 && hasDirectAuthUrl()) {
				try {
					String directUrl = authServiceDirectUrl + registerPath;
					log.info("Gateway returned 404; retrying register at {}", directUrl);
					ResponseEntity<JwtResponse> res = apiClient.postToAbsoluteUrl(directUrl, registerBody, JwtResponse.class);
					if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
						redirectAttributes.addFlashAttribute("success", "Registration successful. You are now logged in.");
						return setJwtAndRedirect(response, res.getBody(), redirectAttributes);
					}
				} catch (Exception ignored) { /* fall through to error */ }
			}
			String body = e.getResponseBodyAsString();
			String msg = extractAuthErrorMessage(body);
			// Under-18: show clear age message instead of generic or "Auth service not reachable"
			if (isUnderAgeError(msg, body)) {
				msg = "You cannot register if you are less than 18 years old.";
			} else if (e.getStatusCode().value() == 404 || (msg != null && msg.trim().equalsIgnoreCase("Not Found"))) {
				msg = "Auth service not reachable. Start Auth service (8081), then open this app at http://localhost:9090.";
			} else if (msg == null || msg.isBlank()) {
				log.warn("Registration failed ({}). Response body (first 200 chars): {}", e.getStatusCode(), body != null && body.length() > 200 ? body.substring(0, 200) + "..." : body);
				msg = "Registration failed. Check: password (min 6 characters), mobile exactly 10 digits, date of birth in the past, you must be 18+, and email not already registered. See server logs for details.";
			}
			redirectAttributes.addFlashAttribute("error", msg);
			preserveRegisterForm(redirectAttributes, request);
			return "redirect:/register";
		} catch (ResourceAccessException e) {
			if (hasDirectAuthUrl()) {
				try {
					String directUrl = authServiceDirectUrl + registerPath;
					log.info("Gateway unreachable; retrying register at {}", directUrl);
					ResponseEntity<JwtResponse> res = apiClient.postToAbsoluteUrl(directUrl, registerBody, JwtResponse.class);
					if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
						redirectAttributes.addFlashAttribute("success", "Registration successful. You are now logged in.");
						return setJwtAndRedirect(response, res.getBody(), redirectAttributes);
					}
				} catch (Exception ignored) { /* fall through */ }
			}
			redirectAttributes.addFlashAttribute("error", "Cannot reach backend. Start Auth service on port 8081, then try again.");
			preserveRegisterForm(redirectAttributes, request);
			return "redirect:/register";
		}
		redirectAttributes.addFlashAttribute("error", "Registration failed.");
		return "redirect:/register";
	}

	private void preserveRegisterForm(RedirectAttributes redirectAttributes, RegisterRequest request) {
		redirectAttributes.addFlashAttribute("name", request.getName());
		redirectAttributes.addFlashAttribute("email", request.getEmail());
		redirectAttributes.addFlashAttribute("mobile", request.getMobile());
		redirectAttributes.addFlashAttribute("address", request.getAddress());
		redirectAttributes.addFlashAttribute("dateOfBirth", request.getDateOfBirth() != null ? request.getDateOfBirth().toString() : null);
	}
}
