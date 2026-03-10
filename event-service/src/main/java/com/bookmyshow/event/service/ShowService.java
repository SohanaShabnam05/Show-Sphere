package com.bookmyshow.event.service;

import com.bookmyshow.event.dto.ReleaseSeatsResponse;
import com.bookmyshow.event.dto.ShowDetailsDto;
import com.bookmyshow.event.dto.ShowRequest;
import com.bookmyshow.event.dto.ShowResponseDto;
import com.bookmyshow.event.dto.ShowSearchRequest;
import com.bookmyshow.event.entity.Show;

import java.util.List;

/**
 * Service contract for Show scheduling and search.
 */
public interface ShowService {

	/**
	 * Creates a show. Fails if same event overlaps at same theater (Admin only).
	 *
	 * @param request show payload
	 * @return created show
	 */
	Show createShow(ShowRequest request);

	/**
	 * Finds show by id.
	 *
	 * @param showId show id
	 * @return show with event and theater
	 */
	Show getShowById(Long showId);

	/**
	 * Returns all shows with full event and theater details.
	 *
	 * @return list of show response DTOs
	 */
	List<ShowResponseDto> getAllShows();

	/**
	 * Returns one show with full event and theater details (for display/booking).
	 *
	 * @param showId show id
	 * @return show response DTO
	 */
	ShowResponseDto getShowFullDetails(Long showId);

	/**
	 * Returns show details for booking (event category, base price, start time, available seats).
	 *
	 * @param showId show id
	 * @return show details DTO
	 */
	ShowDetailsDto getShowDetails(Long showId);

	/**
	 * Search shows by category, event name, date, theater. Returns rich response with event and theater details.
	 *
	 * @param searchRequest search criteria
	 * @return list of show response DTOs
	 */
	List<ShowResponseDto> searchShows(ShowSearchRequest searchRequest);

	/**
	 * Decrements available seats by count. Used by booking-service via Feign.
	 *
	 * @param showId show id
	 * @param count  number of seats to reserve
	 * @return true if successful
	 */
	boolean reserveSeats(Long showId, int count);

	/**
	 * Releases up to reserved seats. Fails if requested count exceeds reserved (totalSeats - availableSeats).
	 *
	 * @param showId show id
	 * @param count  number of seats to release
	 * @return response with releasedCount and message
	 */
	ReleaseSeatsResponse releaseSeats(Long showId, int count);

	/**
	 * Repairs availableSeats for a show so it is within [0, totalSeats]. Use if data was corrupted.
	 *
	 * @param showId show id
	 */
	void repairShowAvailability(Long showId);
}
