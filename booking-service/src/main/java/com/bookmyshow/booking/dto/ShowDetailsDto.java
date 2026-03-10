package com.bookmyshow.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Show details from event-service (for GST and seat check). eventCategory matches EventCategory enum name.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowDetailsDto {

	private Long id;
	private Long eventId;
	private String eventCategory;
	private String eventName;
	private BigDecimal basePrice;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Long theaterId;
	private Integer availableSeats;
}
