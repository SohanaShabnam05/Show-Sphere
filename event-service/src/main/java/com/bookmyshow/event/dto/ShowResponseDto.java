package com.bookmyshow.event.dto;

import com.bookmyshow.event.entity.EventCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rich response for show list, search, and get-by-id. Includes event and theater details.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Show with event and theater details")
public class ShowResponseDto {

	@Schema(description = "Show id (use as showId when booking)")
	private Long id;

	@Schema(description = "Event id")
	private Long eventId;
	@Schema(description = "Event name")
	private String eventName;
	@Schema(description = "Event category: MOVIE, CONCERT, LIVE_SHOW")
	private EventCategory eventCategory;
	@Schema(description = "Base price per seat (before GST)")
	private BigDecimal basePrice;

	@Schema(description = "Theater id")
	private Long theaterId;
	@Schema(description = "Theater name")
	private String theaterName;
	@Schema(description = "Theater address")
	private String theaterAddress;

	@Schema(description = "Show start time")
	private LocalDateTime startTime;
	@Schema(description = "Show end time")
	private LocalDateTime endTime;
	@Schema(description = "Total seats (max occupancy)")
	private Integer totalSeats;
	@Schema(description = "Available seats")
	private Integer availableSeats;
}
