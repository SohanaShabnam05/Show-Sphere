package com.bookmyshow.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response for release seats: how many seats were released and a message.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Release seats result")
public class ReleaseSeatsResponse {

	@Schema(description = "Number of seats released")
	private int releasedCount;

	@Schema(description = "Message, e.g. '2 seats have been released.'")
	private String message;
}
