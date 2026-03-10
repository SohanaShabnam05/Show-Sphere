package com.bookmyshow.event.service.impl;

import com.bookmyshow.event.dto.ReleaseSeatsResponse;
import com.bookmyshow.event.dto.ShowDetailsDto;
import com.bookmyshow.event.dto.ShowRequest;
import com.bookmyshow.event.dto.ShowResponseDto;
import com.bookmyshow.event.dto.ShowSearchRequest;
import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.entity.EventCategory;
import com.bookmyshow.event.entity.Show;
import com.bookmyshow.event.entity.Theater;
import com.bookmyshow.event.exception.EventServiceException;
import com.bookmyshow.event.constant.EventConstants;
import com.bookmyshow.event.repository.EventRepository;
import com.bookmyshow.event.repository.ShowRepository;
import com.bookmyshow.event.repository.TheaterRepository;
import com.bookmyshow.event.service.ShowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ShowService}. Enforces no overlapping same event at one theater.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowServiceImpl implements ShowService {

	private final ShowRepository showRepository;
	private final EventRepository eventRepository;
	private final TheaterRepository theaterRepository;

	@Override
	@Transactional
	public Show createShow(ShowRequest request) {

		log.debug("Creating show for eventId: {}, theaterId: {}", request.getEventId(), request.getTheaterId());

		Event event = eventRepository.findById(request.getEventId())
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_EVENT_NOT_FOUND));
		Theater theater = theaterRepository.findById(request.getTheaterId())
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_THEATER_NOT_FOUND));

		List<Show> overlapping = showRepository.findOverlappingShows(
				request.getTheaterId(), request.getEventId(), request.getStartTime(), request.getEndTime());
		if (!overlapping.isEmpty()) {
			throw new EventServiceException(EventConstants.ERROR_SHOW_OVERLAP);
		}

		Show show = Show.builder()
				.event(event)
				.theater(theater)
				.startTime(request.getStartTime())
				.endTime(request.getEndTime())
				.totalSeats(request.getTotalSeats())
				.availableSeats(request.getTotalSeats())
				.build();
		return showRepository.save(show);
	}

	@Override
	public Show getShowById(Long showId) {

		return showRepository.findById(showId)
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_SHOW_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShowResponseDto> getAllShows() {

		List<Show> shows = showRepository.findAllWithEventAndTheater();
		return shows.stream().map(this::toShowResponseDto).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public ShowResponseDto getShowFullDetails(Long showId) {

		Show show = showRepository.findById(showId)
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_SHOW_NOT_FOUND));
		return toShowResponseDto(show);
	}

	private ShowResponseDto toShowResponseDto(Show show) {

		Event event = show.getEvent();
		Theater theater = show.getTheater();
		int totalSeats = show.getTotalSeats();
		int availableSeats = clampAvailableSeats(show.getAvailableSeats(), totalSeats);
		return ShowResponseDto.builder()
				.id(show.getId())
				.eventId(event.getId())
				.eventName(event.getName())
				.eventCategory(event.getCategory())
				.basePrice(event.getBasePrice())
				.theaterId(theater.getId())
				.theaterName(theater.getName())
				.theaterAddress(theater.getAddress())
				.startTime(show.getStartTime())
				.endTime(show.getEndTime())
				.totalSeats(totalSeats)
				.availableSeats(availableSeats)
				.build();
	}

	/** Ensures availableSeats is in [0, totalSeats] for correct display and consistency. */
	private static int clampAvailableSeats(int availableSeats, int totalSeats) {

		if (availableSeats < 0) {
			return 0;
		}
		return Math.min(availableSeats, totalSeats);
	}

	@Override
	@Transactional(readOnly = true)
	public ShowDetailsDto getShowDetails(Long showId) {

		Show show = showRepository.findById(showId)
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_SHOW_NOT_FOUND));
		Event event = show.getEvent();
		int availableSeats = clampAvailableSeats(show.getAvailableSeats(), show.getTotalSeats());
		return ShowDetailsDto.builder()
				.id(show.getId())
				.eventId(event.getId())
				.eventCategory(event.getCategory())
				.eventName(event.getName())
				.basePrice(event.getBasePrice())
				.startTime(show.getStartTime())
				.endTime(show.getEndTime())
				.theaterId(show.getTheater().getId())
				.availableSeats(availableSeats)
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShowResponseDto> searchShows(ShowSearchRequest searchRequest) {

		EventCategory category = searchRequest.getCategory();
		java.time.LocalDate showDate = searchRequest.getShowDate();
		Long theaterId = searchRequest.getTheaterId();
		String eventName = searchRequest.getEventName();
		List<Show> shows = showRepository.searchShows(category, showDate, theaterId, eventName);
		return shows.stream().map(this::toShowResponseDto).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public boolean reserveSeats(Long showId, int count) {

		Show show = showRepository.findByIdForUpdate(showId)
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_SHOW_NOT_FOUND));
		int available = show.getAvailableSeats();
		log.info("reserveSeats: showId={}, count={}, availableBefore={}, showEntityId={}", showId, count, available, show.getId());
		if (available < count) {
			log.warn("reserveSeats: insufficient seats for showId={}, available={}, requested={}", showId, available, count);
			return false;
		}
		int newAvailable = available - count;
		show.setAvailableSeats(newAvailable);
		showRepository.save(show);
		log.info("reserveSeats: showId={} updated availableSeats {} -> {}", showId, available, newAvailable);
		return true;
	}

	@Override
	@Transactional
	public ReleaseSeatsResponse releaseSeats(Long showId, int count) {

		Show show = showRepository.findByIdForUpdate(showId)
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_SHOW_NOT_FOUND));
		int totalSeats = show.getTotalSeats();
		int currentAvailable = show.getAvailableSeats();
		// Use clamped value to avoid negative or >totalSeats when computing reserved and new availability
		int effectiveAvailable = clampAvailableSeats(currentAvailable, totalSeats);
		int reserved = totalSeats - effectiveAvailable;
		if (reserved > 0 && count > reserved) {
			throw new EventServiceException(
					String.format(EventConstants.ERROR_RELEASE_EXCEEDS_RESERVED, reserved, count));
		}
		// Add released seats and clamp to [0, totalSeats] so cancellation always succeeds and corrupted state is repaired
		int newAvailable = Math.min(effectiveAvailable + count, totalSeats);
		show.setAvailableSeats(newAvailable);
		showRepository.save(show);
		if (currentAvailable != effectiveAvailable || newAvailable != currentAvailable + count) {
			log.info("releaseSeats: showId={} repaired/clamped availableSeats {} -> {}", showId, currentAvailable, newAvailable);
		}
		String message = count == 1
				? "1 seat has been released."
				: count + " seats have been released.";
		return ReleaseSeatsResponse.builder()
				.releasedCount(count)
				.message(message)
				.build();
	}

	@Override
	@Transactional
	public void repairShowAvailability(Long showId) {

		Show show = showRepository.findByIdForUpdate(showId)
				.orElseThrow(() -> new EventServiceException(EventConstants.ERROR_SHOW_NOT_FOUND));
		int totalSeats = show.getTotalSeats();
		int oldAvailable = show.getAvailableSeats();
		int repaired = clampAvailableSeats(oldAvailable, totalSeats);
		if (oldAvailable != repaired) {
			show.setAvailableSeats(repaired);
			showRepository.save(show);
			log.info("Repaired show {} availableSeats: {} -> {}", showId, oldAvailable, repaired);
		}
	}
}
