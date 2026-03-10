package com.bookmyshow.booking.service.impl;

import com.bookmyshow.booking.client.AuthServiceClient;
import com.bookmyshow.booking.client.CouponServiceClient;
import com.bookmyshow.booking.client.EventServiceClient;
import com.bookmyshow.booking.constant.BookingConstants;
import com.bookmyshow.booking.dto.ApplyCouponRequestDto;
import com.bookmyshow.booking.dto.BookingRequest;
import com.bookmyshow.booking.dto.CancelBookingResponse;
import com.bookmyshow.booking.dto.UserDobResponseDto;
import com.bookmyshow.booking.dto.ShowDetailsDto;
import com.bookmyshow.booking.entity.Booking;
import com.bookmyshow.booking.entity.BookingStatus;
import com.bookmyshow.booking.exception.BookingServiceException;
import com.bookmyshow.booking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private EventServiceClient eventServiceClient;

	@Mock
	private CouponServiceClient couponServiceClient;

	@Mock
	private AuthServiceClient authServiceClient;

	@InjectMocks
	private BookingServiceImpl bookingService;

	@Test
	void createBooking_validRequest_shouldPersistBookingWithTotals() {
		// Given
		Long userId = 1L;
		BookingRequest request = BookingRequest.builder()
				.showId(10L)
				.numberOfSeats(2)
				.build();

		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(5L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));

		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(userId, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 2)).thenReturn(true);

		Booking saved = new Booking();
		saved.setId(99L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		Booking result = bookingService.createBooking(userId, request);

		// THEN
		assertThat(result.getId()).isEqualTo(99L);

		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		Booking bookingToSave = captor.getValue();
		assertThat(bookingToSave.getUserId()).isEqualTo(userId);
		assertThat(bookingToSave.getShowId()).isEqualTo(10L);
		assertThat(bookingToSave.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

		// verify
		verify(eventServiceClient).getShowDetails(10L);
		verify(eventServiceClient).reserveSeats(10L, 2);
	}

	@Test
	void testCreateBooking_userAged60Plus_shouldApplyZeroGst() {
		// Given - user DOB 65 years ago => GST exempt
		Long userId = 1L;
		BookingRequest request = BookingRequest.builder().showId(10L).numberOfSeats(1).build();
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(5L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(userId, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 1)).thenReturn(true);
		UserDobResponseDto dobResponse = UserDobResponseDto.builder()
				.dateOfBirth(LocalDate.now().minusYears(65))
				.build();
		when(authServiceClient.getUserDob(userId)).thenReturn(ResponseEntity.ok(dobResponse));
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		bookingService.createBooking(userId, request);

		// Then - GST should be 0 for 60+
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getGstAmount()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
	}

	@Test
	void createBooking_insufficientSeats_shouldThrowBookingServiceException() {
		// Given
		BookingRequest request = BookingRequest.builder()
				.showId(10L)
				.numberOfSeats(5)
				.build();
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(2);
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);

		// When / THEN
		assertThatThrownBy(() -> bookingService.createBooking(1L, request))
				.isInstanceOf(BookingServiceException.class)
				.hasMessage(BookingConstants.ERROR_INSUFFICIENT_SEATS);
	}

	@Test
	void createBooking_couponTooEarly_shouldThrowBookingServiceException() {
		// Given
		BookingRequest request = BookingRequest.builder()
				.showId(10L)
				.numberOfSeats(2)
				.couponCode("SAVE10")
				.build();
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventCategory("MOVIE");
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);

		// When / THEN
		assertThatThrownBy(() -> bookingService.createBooking(1L, request))
				.isInstanceOf(BookingServiceException.class)
				.hasMessage(BookingConstants.ERROR_COUPON_ONLY_AFTER_3_BOOKINGS);
	}

	@Test
	void createBooking_couponServiceThrows_shouldWrapInBookingServiceException() {
		// Given
		Long userId = 1L;
		BookingRequest request = BookingRequest.builder()
				.showId(10L)
				.numberOfSeats(2)
				.couponCode("SAVE10")
				.build();

		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventCategory("MOVIE");
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(userId, BookingStatus.CANCELLED))
				.thenReturn((long) BookingConstants.MIN_BOOKINGS_FOR_COUPON);
		when(couponServiceClient.applyCoupon(any(ApplyCouponRequestDto.class)))
				.thenThrow(new RuntimeException("Coupon invalid"));

		// When / THEN
		assertThatThrownBy(() -> bookingService.createBooking(userId, request))
				.isInstanceOf(BookingServiceException.class)
				.hasMessageContaining("Coupon not found or invalid");
	}

	@Test
	void getMyBookings_upcomingTrue_shouldFilterByStartTimeAndStatus() {
		// Given
		Long userId = 1L;
		LocalDateTime now = LocalDateTime.now();
		Booking upcoming = new Booking();
		upcoming.setShowStartTime(now.plusHours(3));
		upcoming.setStatus(BookingStatus.CONFIRMED);

		Booking cancelled = new Booking();
		cancelled.setShowStartTime(now.plusHours(3));
		cancelled.setStatus(BookingStatus.CANCELLED);

		Booking past = new Booking();
		past.setShowStartTime(now.minusHours(1));
		past.setStatus(BookingStatus.CONFIRMED);

		when(bookingRepository.findByUserIdOrderByBookingTimeDesc(userId))
				.thenReturn(List.of(upcoming, cancelled, past));

		// When
		List<Booking> result = bookingService.getMyBookings(userId, true);

		// THEN
		assertThat(result).containsExactly(upcoming);
	}

	@Test
	void cancelBooking_alreadyCancelled_shouldReturnZeroRefund() {
		// Given
		Long userId = 1L;
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CANCELLED);
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

		// When
		var response = bookingService.cancelBooking(userId, bookingId, null);

		// THEN
		assertThat(response.getRefundAmount()).isZero();
		assertThat(response.getGstRefunded()).isZero();
		assertThat(response.getMessage()).contains("already cancelled");
	}

	@Test
	void cancelBooking_differentUser_shouldThrowBookingServiceException() {
		// Given
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(2L);
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

		// When / THEN
		assertThatThrownBy(() -> bookingService.cancelBooking(1L, bookingId, null))
				.isInstanceOf(BookingServiceException.class)
				.hasMessage(BookingConstants.ERROR_BOOKING_NOT_FOUND);
	}

	@Test
	void cancelBooking_releaseSeatsFails_shouldThrowBookingServiceException() {
		// Given
		Long userId = 1L;
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setShowId(5L);
		booking.setNumberOfSeats(2);
		booking.setShowStartTime(LocalDateTime.now().plusHours(BookingConstants.CANCEL_CUTOFF_HOURS + 1));
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
		doThrow(new RuntimeException("fail")).when(eventServiceClient).releaseSeats(5L, 2);

		// When / THEN
		assertThatThrownBy(() -> bookingService.cancelBooking(userId, bookingId, null))
				.isInstanceOf(BookingServiceException.class)
				.hasMessageContaining("Unable to release seats");
	}

	@Test
	void testCreateBooking_showDetailsNull_shouldThrowShowNotFound() {
		// Given
		when(eventServiceClient.getShowDetails(10L)).thenReturn(null);

		// When / Then
		assertThatThrownBy(() -> bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(1).build()))
				.isInstanceOf(BookingServiceException.class)
				.hasMessage(BookingConstants.ERROR_SHOW_NOT_FOUND);

		// Verify
		verify(eventServiceClient).getShowDetails(10L);
	}

	@Test
	void testCreateBooking_availableSeatsNull_shouldThrowShowNotFound() {
		// Given
		ShowDetailsDto dto = new ShowDetailsDto();
		dto.setAvailableSeats(null);
		dto.setBasePrice(BigDecimal.TEN);
		when(eventServiceClient.getShowDetails(10L)).thenReturn(dto);

		// When / Then
		assertThatThrownBy(() -> bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(1).build()))
				.isInstanceOf(BookingServiceException.class)
				.hasMessage(BookingConstants.ERROR_SHOW_NOT_FOUND);
	}

	@Test
	void testCreateBooking_reserveSeatsReturnsFalse_shouldThrowInsufficientSeats() {
		// Given
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(5);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 2)).thenReturn(false);

		// When / Then
		assertThatThrownBy(() -> bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(2).build()))
				.isInstanceOf(BookingServiceException.class)
				.hasMessage(BookingConstants.ERROR_INSUFFICIENT_SEATS);
	}

	@Test
	void testCreateBooking_showOnSaturday_shouldApply25PercentSurge() {
		// Given - show start on Saturday
		LocalDateTime saturday = LocalDate.of(2026, 3, 7).atStartOfDay().plusHours(14); // 7 Mar 2026 is Saturday
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(saturday);
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 1)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		Booking result = bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(1).build());

		// Then - 100 * 1.25 = 125 subtotal before GST
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		Booking b = captor.getValue();
		assertThat(b.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(135)); // 125 + 8% GST
	}

	@Test
	void testGetMyBookings_noUpcomingFilter_returnsAllFromRepository() {
		// Given
		Long userId = 1L;
		Booking b = new Booking();
		b.setId(1L);
		when(bookingRepository.findByUserIdOrderByBookingTimeDesc(userId)).thenReturn(List.of(b));

		// When
		List<Booking> result = bookingService.getMyBookings(userId);

		// Then
		assertThat(result).containsExactly(b);

		// Verify
		verify(bookingRepository).findByUserIdOrderByBookingTimeDesc(userId);
	}

	@Test
	void testGetMyBookings_upcomingFalse_shouldReturnOnlyPast() {
		// Given
		Long userId = 1L;
		LocalDateTime now = LocalDateTime.now();
		Booking past = new Booking();
		past.setShowStartTime(now.minusHours(2));
		Booking future = new Booking();
		future.setShowStartTime(now.plusHours(2));
		when(bookingRepository.findByUserIdOrderByBookingTimeDesc(userId)).thenReturn(List.of(past, future));

		// When
		List<Booking> result = bookingService.getMyBookings(userId, false);

		// Then
		assertThat(result).containsExactly(past);
	}

	@Test
	void testGetMyBookings_upcomingNull_returnsAll() {
		// Given
		Long userId = 1L;
		Booking b = new Booking();
		when(bookingRepository.findByUserIdOrderByBookingTimeDesc(userId)).thenReturn(List.of(b));

		// When
		List<Booking> result = bookingService.getMyBookings(userId, null);

		// Then
		assertThat(result).containsExactly(b);
	}

	@Test
	void testCancelBooking_showWithinTwoHours_shouldThrowCancelTooLate() {
		// Given
		Long userId = 1L;
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setShowId(5L);
		booking.setNumberOfSeats(2);
		booking.setShowStartTime(LocalDateTime.now().plusHours(1)); // less than 2 hours
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

		// When / Then
		assertThatThrownBy(() -> bookingService.cancelBooking(userId, bookingId, null))
				.isInstanceOf(BookingServiceException.class)
				.hasMessage(BookingConstants.ERROR_CANCEL_TOO_LATE);

		// Verify
		verify(bookingRepository).findById(bookingId);
		verify(eventServiceClient, never()).releaseSeats(any(), anyInt());
	}

	@Test
	void testCancelBooking_partialSeatsToCancel_shouldSetPartialCancelled() {
		// Given
		Long userId = 1L;
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setShowId(5L);
		booking.setNumberOfSeats(4);
		booking.setTotalAmount(BigDecimal.valueOf(400));
		booking.setGstAmount(BigDecimal.valueOf(40));
		booking.setShowStartTime(LocalDateTime.now().plusHours(BookingConstants.CANCEL_CUTOFF_HOURS + 1));
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

		// When
		CancelBookingResponse response = bookingService.cancelBooking(userId, bookingId, 2);

		// Then
		assertThat(response.getRefundAmount()).isGreaterThan(BigDecimal.ZERO);
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.PARTIAL_CANCELLED);
		assertThat(captor.getValue().getNumberOfSeats()).isEqualTo(2);

		// Verify
		verify(eventServiceClient).releaseSeats(5L, 2);
	}

	@Test
	void testHasBookingsByShowId_delegatesToRepository() {
		// Given
		Long showId = 5L;
		when(bookingRepository.existsByShowId(showId)).thenReturn(true);

		// When
		boolean result = bookingService.hasBookingsByShowId(showId);

		// Then
		assertThat(result).isTrue();

		// Verify
		verify(bookingRepository).existsByShowId(showId);
	}

	@Test
	void testGetReportCsv_nullDates_shouldStillBuildCsv() {
		// Given
		when(bookingRepository.findForReport(null, null, null)).thenReturn(Collections.emptyList());

		// When
		String csv = bookingService.getReportCsv(null, null, null);

		// Then
		assertThat(csv).contains("bookingId,userId,eventId,showId");
		assertThat(csv).contains("TOTALS");
	}

	@Test
	void testCancelBooking_bookingNotFound_shouldThrow() {
		// Given
		when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

		// When / Then
		assertThatThrownBy(() -> bookingService.cancelBooking(1L, 999L, null))
				.isInstanceOf(BookingServiceException.class)
				.hasMessage(BookingConstants.ERROR_BOOKING_NOT_FOUND);
	}

	@Test
	void getReportCsv_shouldIncludeTotalsAndLines() {
		// Given
		Booking booking = new Booking();
		booking.setId(1L);
		booking.setUserId(2L);
		booking.setEventId(3L);
		booking.setShowId(4L);
		booking.setNumberOfSeats(2);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setBookingTime(LocalDateTime.now());
		booking.setShowStartTime(LocalDateTime.now().plusDays(1));
		booking.setTotalAmount(BigDecimal.valueOf(200));
		booking.setGstAmount(BigDecimal.valueOf(36));
		booking.setDiscountAmount(BigDecimal.ZERO);
		booking.setCouponCode("SAVE10");

		when(bookingRepository.findForReport(any(), any(), any())).thenReturn(List.of(booking));

		// When
		String csv = bookingService.getReportCsv(3L, LocalDate.now(), LocalDate.now());

		// THEN
		assertThat(csv).contains("bookingId,userId,eventId,showId");
		assertThat(csv).contains("TOTALS");
		assertThat(csv).contains("SAVE10");
	}

	@Test
	void createBooking_basePriceNull_shouldUseZeroAndSucceed() {
		// Given
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(5);
		showDetails.setBasePrice(null);
		showDetails.setEventId(1L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 1)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		Booking result = bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(1).build());

		// Then
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void createBooking_showStartTimeNull_shouldNotApplyWeekendSurge() {
		// Given
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(null);
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 1)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(1).build());

		// Then - no surge, 100 + 8% GST = 108
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(108));
	}

	@Test
	void createBooking_couponAppliedWithNullFinalAmount_shouldUseSubtotalAndProceed() {
		// Given
		Long userId = 1L;
		BookingRequest request = BookingRequest.builder()
				.showId(10L)
				.numberOfSeats(2)
				.couponCode("SAVE10")
				.build();
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(userId, BookingStatus.CANCELLED))
				.thenReturn((long) BookingConstants.MIN_BOOKINGS_FOR_COUPON);
		com.bookmyshow.booking.dto.ApplyCouponResponseDto couponResponse =
				com.bookmyshow.booking.dto.ApplyCouponResponseDto.builder()
						.finalAmount(null)
						.discountAmount(BigDecimal.valueOf(20))
						.build();
		when(couponServiceClient.applyCoupon(any(ApplyCouponRequestDto.class))).thenReturn(couponResponse);
		when(eventServiceClient.reserveSeats(10L, 2)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		bookingService.createBooking(userId, request);

		// Then - amountAfterDiscount stays subtotal when finalAmount null
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getCouponCode()).isEqualTo("SAVE10");
	}

	@Test
	void createBooking_eventCategoryConcert_shouldApply10PercentGst() {
		// Given
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("CONCERT");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 1)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(1).build());

		// Then - 100 + 10% GST = 110
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(110));
		assertThat(captor.getValue().getGstAmount()).isEqualByComparingTo(BigDecimal.TEN);
	}

	@Test
	void createBooking_eventCategoryLiveShow_shouldApply6PercentGst() {
		// Given
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("LIVE_SHOW");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 1)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(1).build());

		// Then - 100 + 6% GST = 106
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(106));
	}

	@Test
	void createBooking_authClientThrows_shouldFallbackToDefaultGst() {
		// Given
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);
		when(authServiceClient.getUserDob(1L)).thenThrow(new RuntimeException("auth down"));
		when(eventServiceClient.reserveSeats(10L, 1)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		bookingService.createBooking(1L, BookingRequest.builder().showId(10L).numberOfSeats(1).build());

		// Then - MOVIE 8% GST applied
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(108));
	}

	@Test
	void getReportCsv_bookingWithNullFields_shouldEscapeAndIncludeInCsv() {
		// Given - booking with null eventId, null bookingTime, null showStartTime, coupon with comma
		Booking booking = new Booking();
		booking.setId(2L);
		booking.setUserId(3L);
		booking.setEventId(null);
		booking.setShowId(4L);
		booking.setNumberOfSeats(1);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setBookingTime(null);
		booking.setShowStartTime(null);
		booking.setTotalAmount(null);
		booking.setGstAmount(null);
		booking.setDiscountAmount(BigDecimal.ZERO);
		booking.setCouponCode("SAVE,20");
		when(bookingRepository.findForReport(any(), any(), any())).thenReturn(List.of(booking));

		// When
		String csv = bookingService.getReportCsv(1L, LocalDate.now(), LocalDate.now());

		// Then - CSV escapes coupon field with quotes
		assertThat(csv).contains("bookingId,userId,eventId,showId");
		assertThat(csv).contains("\"SAVE,20\"");
	}

	@Test
	void cancelBooking_fullCancel_shouldSetStatusCancelledAndSeatsZero() {
		// Given
		Long userId = 1L;
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setShowId(5L);
		booking.setNumberOfSeats(3);
		booking.setTotalAmount(BigDecimal.valueOf(300));
		booking.setGstAmount(BigDecimal.ZERO);
		booking.setShowStartTime(LocalDateTime.now().plusHours(BookingConstants.CANCEL_CUTOFF_HOURS + 1));
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

		// When
		CancelBookingResponse response = bookingService.cancelBooking(userId, bookingId, 3);

		// Then
		assertThat(response.getRefundAmount()).isGreaterThan(BigDecimal.ZERO);
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(captor.getValue().getNumberOfSeats()).isEqualTo(0);
		verify(eventServiceClient).releaseSeats(5L, 3);
	}

	@Test
	void getMyBookings_upcomingTrue_excludesNullShowStartTime() {
		// Given
		Long userId = 1L;
		Booking withNullStart = new Booking();
		withNullStart.setShowStartTime(null);
		withNullStart.setStatus(BookingStatus.CONFIRMED);
		Booking upcoming = new Booking();
		upcoming.setShowStartTime(LocalDateTime.now().plusHours(2));
		upcoming.setStatus(BookingStatus.CONFIRMED);
		when(bookingRepository.findByUserIdOrderByBookingTimeDesc(userId))
				.thenReturn(List.of(withNullStart, upcoming));

		// When
		List<Booking> result = bookingService.getMyBookings(userId, true);

		// Then
		assertThat(result).containsExactly(upcoming);
	}

	@Test
	void getMyBookings_upcomingFalse_excludesNullShowStartTime() {
		// Given
		Long userId = 1L;
		LocalDateTime now = LocalDateTime.now();
		Booking withNullStart = new Booking();
		withNullStart.setShowStartTime(null);
		Booking past = new Booking();
		past.setShowStartTime(now.minusHours(1));
		when(bookingRepository.findByUserIdOrderByBookingTimeDesc(userId))
				.thenReturn(List.of(withNullStart, past));

		// When
		List<Booking> result = bookingService.getMyBookings(userId, false);

		// Then
		assertThat(result).containsExactly(past);
	}

	@Test
	void testHasBookingsByShowId_noBookings_returnsFalse() {
		// Given
		when(bookingRepository.existsByShowId(7L)).thenReturn(false);

		// When
		boolean result = bookingService.hasBookingsByShowId(7L);

		// Then
		assertThat(result).isFalse();
		verify(bookingRepository).existsByShowId(7L);
	}

	@Test
	void cancelBooking_showStartBetween12And24Hours_shouldApply50PercentRefund() {
		// Given
		Long userId = 1L;
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setShowId(5L);
		booking.setNumberOfSeats(2);
		booking.setTotalAmount(BigDecimal.valueOf(200));
		booking.setGstAmount(BigDecimal.valueOf(16));
		booking.setShowStartTime(LocalDateTime.now().plusHours(18));
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

		// When
		CancelBookingResponse response = bookingService.cancelBooking(userId, bookingId, null);

		// Then - 50% refund
		assertThat(response.getRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
		verify(eventServiceClient).releaseSeats(5L, 2);
	}

	@Test
	void cancelBooking_showStartBetween2And12Hours_shouldApply10PercentRefund() {
		// Given
		Long userId = 1L;
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setShowId(5L);
		booking.setNumberOfSeats(2);
		booking.setTotalAmount(BigDecimal.valueOf(200));
		booking.setGstAmount(BigDecimal.valueOf(16));
		booking.setShowStartTime(LocalDateTime.now().plusHours(5));
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

		// When
		CancelBookingResponse response = bookingService.cancelBooking(userId, bookingId, null);

		// Then - 10% refund
		assertThat(response.getRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(20));
		verify(eventServiceClient).releaseSeats(5L, 2);
	}

	@Test
	void createBooking_blankCouponCode_shouldNotCallCouponService() {
		// Given
		BookingRequest request = BookingRequest.builder()
				.showId(10L)
				.numberOfSeats(1)
				.couponCode("   ")
				.build();
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(1L, BookingStatus.CANCELLED)).thenReturn(0L);
		when(eventServiceClient.reserveSeats(10L, 1)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		bookingService.createBooking(1L, request);

		// Then - no coupon applied
		verify(couponServiceClient, never()).applyCoupon(any());
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getCouponCode()).isNull();
	}

	@Test
	void createBooking_couponReturnsNull_shouldProceedWithoutDiscount() {
		// Given
		Long userId = 1L;
		BookingRequest request = BookingRequest.builder()
				.showId(10L)
				.numberOfSeats(2)
				.couponCode("SAVE10")
				.build();
		ShowDetailsDto showDetails = new ShowDetailsDto();
		showDetails.setAvailableSeats(10);
		showDetails.setBasePrice(BigDecimal.valueOf(100));
		showDetails.setEventId(1L);
		showDetails.setEventCategory("MOVIE");
		showDetails.setStartTime(LocalDateTime.now().plusDays(1));
		when(eventServiceClient.getShowDetails(10L)).thenReturn(showDetails);
		when(bookingRepository.countByUserIdAndStatusNot(userId, BookingStatus.CANCELLED))
				.thenReturn((long) BookingConstants.MIN_BOOKINGS_FOR_COUPON);
		when(couponServiceClient.applyCoupon(any(ApplyCouponRequestDto.class))).thenReturn(null);
		when(eventServiceClient.reserveSeats(10L, 2)).thenReturn(true);
		Booking saved = new Booking();
		saved.setId(1L);
		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

		// When
		bookingService.createBooking(userId, request);

		// Then - no discount applied when response null
		ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(captor.capture());
		assertThat(captor.getValue().getCouponCode()).isNull();
		assertThat(captor.getValue().getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getReportCsv_couponCodeWithNewline_shouldEscapeWithQuotes() {
		// Given
		Booking booking = new Booking();
		booking.setId(1L);
		booking.setUserId(1L);
		booking.setEventId(1L);
		booking.setShowId(1L);
		booking.setNumberOfSeats(1);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setTotalAmount(BigDecimal.ONE);
		booking.setGstAmount(BigDecimal.ZERO);
		booking.setDiscountAmount(BigDecimal.ZERO);
		booking.setCouponCode("SAVE\n10");
		when(bookingRepository.findForReport(any(), any(), any())).thenReturn(List.of(booking));

		// When
		String csv = bookingService.getReportCsv(1L, LocalDate.now(), LocalDate.now());

		// Then
		assertThat(csv).contains("\"SAVE\n10\"");
	}

	@Test
	void getReportCsv_couponCodeWithDoubleQuote_shouldEscapeDoubled() {
		// Given
		Booking booking = new Booking();
		booking.setId(1L);
		booking.setUserId(1L);
		booking.setEventId(1L);
		booking.setShowId(1L);
		booking.setNumberOfSeats(1);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setTotalAmount(BigDecimal.ONE);
		booking.setGstAmount(BigDecimal.ZERO);
		booking.setDiscountAmount(BigDecimal.ZERO);
		booking.setCouponCode("SAVE\"20");
		when(bookingRepository.findForReport(any(), any(), any())).thenReturn(List.of(booking));

		// When
		String csv = bookingService.getReportCsv(1L, LocalDate.now(), LocalDate.now());

		// Then
		assertThat(csv).contains("\"SAVE\"\"20\"");
	}

	@Test
	void cancelBooking_showStartNull_shouldAllowCancelWith24PlusRefund() {
		// Given
		Long userId = 1L;
		Long bookingId = 10L;
		Booking booking = new Booking();
		booking.setId(bookingId);
		booking.setUserId(userId);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setShowId(5L);
		booking.setNumberOfSeats(2);
		booking.setTotalAmount(BigDecimal.valueOf(200));
		booking.setGstAmount(BigDecimal.valueOf(16));
		booking.setShowStartTime(null);
		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

		// When
		CancelBookingResponse response = bookingService.cancelBooking(userId, bookingId, null);

		// Then - getRefundPercent(showStart null) returns REFUND_PERCENT_24_PLUS
		assertThat(response.getRefundAmount()).isGreaterThan(BigDecimal.ZERO);
		assertThat(response.getGstRefunded()).isGreaterThan(BigDecimal.ZERO);
		verify(eventServiceClient).releaseSeats(5L, 2);
	}
}

