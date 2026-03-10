package com.bookmyshow.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating a theater (venue).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheaterRequest {

	@NotBlank(message = "Theater name is required")
	@Schema(example = "PVR Cinemas - Forum Mall")
	private String name;

	@Schema(example = "Koramangala, Bangalore")
	private String address;
}
