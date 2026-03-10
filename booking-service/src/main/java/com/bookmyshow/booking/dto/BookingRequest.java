package com.bookmyshow.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

	@NotNull(message = "Show id is required")
	private Long showId;

	@NotNull(message = "Number of seats is required")
	@Min(value = 1, message = "At least 1 seat required")
	private Integer numberOfSeats;

	private String couponCode;
}
