package com.bookmyshow.event.dto;

import com.bookmyshow.event.entity.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSearchRequest {

	private EventCategory category;
	private LocalDate showDate;
	private Long theaterId;
	/**
	 * Event name (partial match); optional.
	 */
	private String eventName;
}
