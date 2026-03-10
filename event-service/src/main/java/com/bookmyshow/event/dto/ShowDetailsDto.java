package com.bookmyshow.event.dto;

import com.bookmyshow.event.entity.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Show details for booking service (event category, base price, start time, available seats).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowDetailsDto {

	private Long id;
	private Long eventId;
	private EventCategory eventCategory;
	private String eventName;
	private BigDecimal basePrice;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Long theaterId;
	private Integer availableSeats;
}
