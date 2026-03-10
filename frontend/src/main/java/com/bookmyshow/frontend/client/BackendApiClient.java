package com.bookmyshow.frontend.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BackendApiClient {

	private final RestTemplate restTemplate;
	private final String baseUrl;

	public BackendApiClient(RestTemplate restTemplate, @Qualifier("backendBaseUrl") String baseUrl) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
	}

	public <T> ResponseEntity<T> get(String path, Class<T> responseType) {
		return restTemplate.exchange(baseUrl + path, HttpMethod.GET, null, responseType);
	}

	/** GET from an absolute URL (e.g. direct event-service fallback). */
	public <T> ResponseEntity<T> getAbsoluteUrl(String fullUrl, Class<T> responseType) {
		return restTemplate.exchange(fullUrl, HttpMethod.GET, null, responseType);
	}

	/** POST with JSON body (Content-Type: application/json). Used for auth register/login. */
	public <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> entity = new HttpEntity<>(body, headers);
		return restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, responseType);
	}

	/** POST to an absolute URL (e.g. direct auth-service fallback). Same JSON body. */
	public <T> ResponseEntity<T> postToAbsoluteUrl(String fullUrl, Object body, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Object> entity = new HttpEntity<>(body, headers);
		return restTemplate.exchange(fullUrl, HttpMethod.POST, entity, responseType);
	}

	/** POST with no body (e.g. reserve/release seats). Path may include query params. */
	public <T> ResponseEntity<T> postNoBody(String path, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		return restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, responseType);
	}

	/** POST with no body to an absolute URL (e.g. direct event-service reserve/release). */
	public <T> ResponseEntity<T> postNoBodyAbsoluteUrl(String fullUrl, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		return restTemplate.exchange(fullUrl, HttpMethod.POST, entity, responseType);
	}

	public void delete(String path) {
		restTemplate.delete(baseUrl + path);
	}

	/** DELETE to an absolute URL (e.g. direct event-service fallback). */
	public void deleteAbsoluteUrl(String fullUrl) {
		restTemplate.delete(fullUrl);
	}

	public <T> ResponseEntity<T> exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
		HttpEntity<Object> entity = body != null ? new HttpEntity<>(body) : null;
		return restTemplate.exchange(baseUrl + path, method, entity, responseType);
	}

	public String getCsv(String pathWithQuery) {
		return restTemplate.getForObject(baseUrl + pathWithQuery, String.class);
	}

	/** GET CSV from an absolute URL (e.g. direct booking-service for reports). */
	public String getCsvAbsoluteUrl(String fullUrl) {
		return restTemplate.getForObject(fullUrl, String.class);
	}
}
