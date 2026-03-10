package com.bookmyshow.event.service;

import com.bookmyshow.event.dto.TheaterRequest;
import com.bookmyshow.event.entity.Theater;

import java.util.List;

/**
 * Service contract for Theater operations.
 */
public interface TheaterService {

	Theater createTheater(TheaterRequest request);

	List<Theater> getAllTheaters();

	Theater getTheaterById(Long theaterId);
}
