package com.bookmyshow.event.service.impl;

import com.bookmyshow.event.dto.TheaterRequest;
import com.bookmyshow.event.entity.Theater;
import com.bookmyshow.event.exception.EventServiceException;
import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.repository.TheaterRepository;
import com.bookmyshow.event.service.TheaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link TheaterService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TheaterServiceImpl implements TheaterService {

	private final TheaterRepository theaterRepository;

	@Override
	@Transactional
	public Theater createTheater(TheaterRequest request) {

		log.debug("Creating theater: {}", request.getName());
		Theater theater = Theater.builder()
				.name(request.getName())
				.address(request.getAddress())
				.build();
		return theaterRepository.save(theater);
	}

	@Override
	public List<Theater> getAllTheaters() {

		return theaterRepository.findAll();
	}

	@Override
	public Theater getTheaterById(Long theaterId) {

		return theaterRepository.findById(theaterId)
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_THEATER_NOT_FOUND));
	}
}
