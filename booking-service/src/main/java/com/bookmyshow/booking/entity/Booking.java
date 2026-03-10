package com.bookmyshow.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long showId;

	@Column(name = "event_id")
	private Long eventId;

	@Column(nullable = false)
	private Integer numberOfSeats;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status;

	@Column(nullable = false)
	private LocalDateTime bookingTime;

	private String couponCode;

	/** Show start time (for cancellation refund rules and upcoming/past). */
	@Column(name = "show_start_time")
	private LocalDateTime showStartTime;

	@Column(name = "gst_amount", precision = 10, scale = 2)
	private BigDecimal gstAmount;

	@Column(name = "discount_amount", precision = 10, scale = 2)
	private BigDecimal discountAmount;
}
