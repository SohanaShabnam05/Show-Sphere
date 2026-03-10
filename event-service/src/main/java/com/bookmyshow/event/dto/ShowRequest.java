package com.bookmyshow.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowRequest {

	@NotNull(message = "Event id is required")
	@Schema(example = "1")
	private Long eventId;

	@NotNull(message = "Theater id is required")
	@Schema(example = "1")
	private Long theaterId;

	@NotNull(message = "Start time is required")
	@Schema(example = "2026-06-20T10:00:00")
	private LocalDateTime startTime;

	@NotNull(message = "End time is required")
	@Schema(example = "2026-06-20T13:00:00")
	private LocalDateTime endTime;

	@NotNull(message = "Total seats is required")
	@Min(value = 1, message = "Total seats must be at least 1")
	@Schema(example = "100")
	private Integer totalSeats;
}
