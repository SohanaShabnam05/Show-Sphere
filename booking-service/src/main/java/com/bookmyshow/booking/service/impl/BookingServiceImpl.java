package com.bookmyshow.booking.service.impl;

import com.bookmyshow.booking.client.AuthServiceClient;
import com.bookmyshow.booking.client.CouponServiceClient;
import com.bookmyshow.booking.client.EventServiceClient;
import com.bookmyshow.booking.constant.BookingConstants;
import com.bookmyshow.booking.dto.ApplyCouponRequestDto;
import com.bookmyshow.booking.dto.BookingRequest;
import com.bookmyshow.booking.dto.CancelBookingResponse;
import com.bookmyshow.booking.dto.ShowDetailsDto;
import com.bookmyshow.booking.entity.Booking;
import com.bookmyshow.booking.entity.BookingStatus;
import com.bookmyshow.booking.exception.BookingServiceException;
import com.bookmyshow.booking.repository.BookingRepository;
import com.bookmyshow.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final EventServiceClient eventServiceClient;
	private final CouponServiceClient couponServiceClient;
	private final AuthServiceClient authServiceClient;

	@Override
	@Transactional
	public Booking createBooking(Long userId, BookingRequest request) {

		log.debug("Creating booking for userId: {}, showId: {}", userId, request.getShowId());

		ShowDetailsDto showDetails = eventServiceClient.getShowDetails(request.getShowId());
		if (showDetails == null || showDetails.getAvailableSeats() == null) {
			throw new BookingServiceException(BookingConstants.ERROR_SHOW_NOT_FOUND);
		}
		if (showDetails.getAvailableSeats() < request.getNumberOfSeats()) {
			throw new BookingServiceException(BookingConstants.ERROR_INSUFFICIENT_SEATS);
		}

		BigDecimal basePrice = showDetails.getBasePrice() != null ? showDetails.getBasePrice() : BigDecimal.ZERO;
		BigDecimal subtotal = basePrice
				.multiply(BigDecimal.valueOf(request.getNumberOfSeats()))
				.setScale(2, RoundingMode.HALF_UP);

		// Weekend surge: 25% on base when the show date is Saturday or Sunday (not booking date)
		LocalDateTime showStart = showDetails.getStartTime();
		DayOfWeek showDay = showStart != null ? showStart.toLocalDate().getDayOfWeek() : null;
		if (showDay == DayOfWeek.SATURDAY || showDay == DayOfWeek.SUNDAY) {
			subtotal = subtotal
					.multiply(BigDecimal.valueOf(1.25))
					.setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal amountAfterDiscount = subtotal;
		String couponCode = null;
		BigDecimal discountAmount = BigDecimal.ZERO;

		long priorCompletedBookings = bookingRepository.countByUserIdAndStatusNot(userId, BookingStatus.CANCELLED);
		if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
			if (priorCompletedBookings < BookingConstants.MIN_BOOKINGS_FOR_COUPON) {
				throw new BookingServiceException(BookingConstants.ERROR_COUPON_ONLY_AFTER_3_BOOKINGS);
			}
			ApplyCouponRequestDto couponRequest = ApplyCouponRequestDto.builder()
					.couponCode(request.getCouponCode())
					.amount(subtotal)
					.eventCategory(showDetails.getEventCategory())
					.build();
			try {
				com.bookmyshow.booking.dto.ApplyCouponResponseDto couponResponse = couponServiceClient.applyCoupon(couponRequest);
				if (couponResponse != null) {
					amountAfterDiscount = couponResponse.getFinalAmount() != null ? couponResponse.getFinalAmount() : amountAfterDiscount;
					discountAmount = couponResponse.getDiscountAmount() != null ? couponResponse.getDiscountAmount() : BigDecimal.ZERO;
					couponCode = request.getCouponCode();
				}
			} catch (Exception e) {
				log.warn("Coupon apply failed", e);
				// Hide Feign internal message and return a clean, user-friendly error
				throw new BookingServiceException("Coupon not found or invalid. Please check the code or try again after 3 completed bookings.");
			}
		}

		int gstPercent = getGstPercent(userId, showDetails.getEventCategory());
		BigDecimal gstAmount = amountAfterDiscount.multiply(BigDecimal.valueOf(gstPercent))
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		BigDecimal totalAmount = amountAfterDiscount.add(gstAmount).setScale(2, RoundingMode.HALF_UP);

		boolean reserved = eventServiceClient.reserveSeats(request.getShowId(), request.getNumberOfSeats());
		if (!reserved) {
			throw new BookingServiceException(BookingConstants.ERROR_INSUFFICIENT_SEATS);
		}

		Booking booking = Booking.builder()
				.userId(userId)
				.showId(request.getShowId())
				.eventId(showDetails.getEventId())
				.numberOfSeats(request.getNumberOfSeats())
				.totalAmount(totalAmount)
				.status(BookingStatus.CONFIRMED)
				.bookingTime(LocalDateTime.now())
				.couponCode(couponCode)
				.showStartTime(showDetails.getStartTime())
				.gstAmount(gstAmount)
				.discountAmount(discountAmount)
				.build();
		return bookingRepository.save(booking);
	}

	private int getGstPercent(Long userId, String eventCategory) {

		try {
			ResponseEntity<com.bookmyshow.booking.dto.UserDobResponseDto> resp = authServiceClient.getUserDob(userId);
			if (resp != null && resp.getBody() != null && resp.getBody().getDateOfBirth() != null) {
				LocalDate dob = resp.getBody().getDateOfBirth();
				int age = (int) ChronoUnit.YEARS.between(dob, LocalDate.now());
				if (age >= BookingConstants.GST_EXEMPT_AGE_YEARS) {
					return 0;
				}
			}
		} catch (Exception e) {
			log.debug("Could not get user DOB for GST check: {}", e.getMessage());
		}
		if ("MOVIE".equalsIgnoreCase(eventCategory)) {
			return BookingConstants.GST_PERCENT_MOVIE;
		}
		if ("CONCERT".equalsIgnoreCase(eventCategory)) {
			return BookingConstants.GST_PERCENT_CONCERT;
		}
		if ("LIVE_SHOW".equalsIgnoreCase(eventCategory)) {
			return BookingConstants.GST_PERCENT_LIVE_SHOW;
		}
		return BookingConstants.GST_PERCENT_MOVIE;
	}

	@Override
	public List<Booking> getMyBookings(Long userId) {

		return bookingRepository.findByUserIdOrderByBookingTimeDesc(userId);
	}

	@Override
	public List<Booking> getMyBookings(Long userId, Boolean upcoming) {

		List<Booking> all = bookingRepository.findByUserIdOrderByBookingTimeDesc(userId);
		LocalDateTime now = LocalDateTime.now();
		if (Boolean.TRUE.equals(upcoming)) {
			return all.stream()
					.filter(b -> b.getShowStartTime() != null && b.getShowStartTime().isAfter(now) && b.getStatus() != BookingStatus.CANCELLED)
					.collect(Collectors.toList());
		}
		if (Boolean.FALSE.equals(upcoming)) {
			return all.stream()
					.filter(b -> b.getShowStartTime() != null && !b.getShowStartTime().isAfter(now))
					.collect(Collectors.toList());
		}
		return all;
	}

	@Override
	@Transactional
	public CancelBookingResponse cancelBooking(Long userId, Long bookingId, Integer seatsToCancel) {

		log.debug("Cancel booking id: {} for userId: {}, seats: {}", bookingId, userId, seatsToCancel);

		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new BookingServiceException(BookingConstants.ERROR_BOOKING_NOT_FOUND));
		if (!booking.getUserId().equals(userId)) {
			throw new BookingServiceException(BookingConstants.ERROR_BOOKING_NOT_FOUND);
		}
		if (booking.getStatus() == BookingStatus.CANCELLED) {
			return CancelBookingResponse.builder()
					.refundAmount(BigDecimal.ZERO)
					.gstRefunded(BigDecimal.ZERO)
					.message("Booking already cancelled.")
					.build();
		}

		LocalDateTime showStart = booking.getShowStartTime();
		if (showStart != null) {
			long hoursUntilStart = ChronoUnit.HOURS.between(LocalDateTime.now(), showStart);
			if (hoursUntilStart < BookingConstants.CANCEL_CUTOFF_HOURS) {
				throw new BookingServiceException(BookingConstants.ERROR_CANCEL_TOO_LATE);
			}
		}

		int toRelease = seatsToCancel != null && seatsToCancel > 0
				? Math.min(seatsToCancel, booking.getNumberOfSeats())
				: booking.getNumberOfSeats();

		double refundPercent = getRefundPercent(showStart);
		BigDecimal totalForRefund = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
		BigDecimal gstAmount = booking.getGstAmount() != null ? booking.getGstAmount() : BigDecimal.ZERO;
		BigDecimal refundAmount = totalForRefund.multiply(BigDecimal.valueOf(refundPercent))
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		BigDecimal gstRefunded = BigDecimal.ZERO;
		if (totalForRefund.compareTo(BigDecimal.ZERO) > 0 && gstAmount.compareTo(BigDecimal.ZERO) > 0) {
			gstRefunded = gstAmount.multiply(BigDecimal.valueOf(refundPercent))
					.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		}

		try {
			eventServiceClient.releaseSeats(booking.getShowId(), toRelease);
		} catch (Exception e) {
			log.warn("Event service releaseSeats failed for showId: {}, count: {}", booking.getShowId(), toRelease, e);
			throw new BookingServiceException("Unable to release seats. Please try again or contact support.");
		}
		if (toRelease >= booking.getNumberOfSeats()) {
			booking.setStatus(BookingStatus.CANCELLED);
			booking.setNumberOfSeats(0);
		} else {
			booking.setStatus(BookingStatus.PARTIAL_CANCELLED);
			booking.setNumberOfSeats(booking.getNumberOfSeats() - toRelease);
		}
		bookingRepository.save(booking);

		return CancelBookingResponse.builder()
				.refundAmount(refundAmount)
				.gstRefunded(gstRefunded)
				.message(String.format("Refund: %s (GST: %s). %d%% refund as per policy.", refundAmount, gstRefunded, (int) refundPercent))
				.build();
	}

	private double getRefundPercent(LocalDateTime showStart) {

		if (showStart == null) {
			return BookingConstants.REFUND_PERCENT_24_PLUS;
		}
		long hoursUntilStart = ChronoUnit.HOURS.between(LocalDateTime.now(), showStart);
		if (hoursUntilStart >= 24) {
			return BookingConstants.REFUND_PERCENT_24_PLUS;
		}
		if (hoursUntilStart >= 12) {
			return BookingConstants.REFUND_PERCENT_12_24_HRS;
		}
		if (hoursUntilStart >= 2) {
			return BookingConstants.REFUND_PERCENT_2_12_HRS;
		}
		return 0;
	}

	@Override
	public boolean hasBookingsByShowId(Long showId) {

		return bookingRepository.existsByShowId(showId);
	}
	@Override
	public String getReportCsv(Long eventId, LocalDate fromDate, LocalDate toDate) {

		LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
		LocalDateTime to = toDate != null ? toDate.atTime(23, 59, 59) : null;

		List<Booking> bookings = bookingRepository.findForReport(eventId, from, to);

		long totalBookings = bookings.stream()
				.filter(b -> b.getStatus() != BookingStatus.CANCELLED)
				.count();
		BigDecimal totalAmount = bookings.stream()
				.filter(b -> b.getStatus() != BookingStatus.CANCELLED)
				.map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalGst = bookings.stream()
				.filter(b -> b.getStatus() != BookingStatus.CANCELLED)
				.map(b -> b.getGstAmount() != null ? b.getGstAmount() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		StringBuilder sb = new StringBuilder();
		sb.append("bookingId,userId,eventId,showId,numberOfSeats,status,bookingTime,showStartTime,totalAmount,gstAmount,discountAmount,couponCode\n");
		for (Booking b : bookings) {
			sb.append(b.getId()).append(',')
					.append(b.getUserId()).append(',')
					.append(b.getEventId() != null ? b.getEventId() : "").append(',')
					.append(b.getShowId()).append(',')
					.append(b.getNumberOfSeats()).append(',')
					.append(b.getStatus()).append(',')
					.append(b.getBookingTime() != null ? b.getBookingTime() : "").append(',')
					.append(b.getShowStartTime() != null ? b.getShowStartTime() : "").append(',')
					.append(b.getTotalAmount()).append(',')
					.append(b.getGstAmount()).append(',')
					.append(b.getDiscountAmount()).append(',')
					.append(escapeCsvField(b.getCouponCode())).append('\n');
		}
		sb.append("TOTALS,,,,,,,")
				.append(totalBookings).append(',')
				.append(totalAmount).append(',')
				.append(totalGst).append(",\n");
		return sb.toString();
	}

	/** Escapes a CSV field (quotes if contains comma, newline or double quote). */
	private static String escapeCsvField(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}

