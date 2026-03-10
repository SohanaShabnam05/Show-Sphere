package com.bookmyshow.event.dto;

import com.bookmyshow.event.entity.EventCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

	@NotBlank(message = "Event name is required")
	@Schema(example = "Avengers Endgame")
	private String name;

	@NotNull(message = "Category is required")
	@Schema(example = "MOVIE", allowableValues = { "MOVIE", "CONCERT", "LIVE_SHOW" })
	private EventCategory category;

	@NotNull(message = "Base price is required")
	@DecimalMin(value = "0", message = "Price must be non-negative")
	@Schema(example = "250.00")
	private BigDecimal basePrice;
}
