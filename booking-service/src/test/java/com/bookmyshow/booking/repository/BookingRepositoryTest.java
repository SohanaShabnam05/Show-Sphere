package com.bookmyshow.booking.repository;

import com.bookmyshow.booking.entity.Booking;
import com.bookmyshow.booking.entity.BookingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookingRepositoryTest {

	@Autowired
	private BookingRepository bookingRepository;

	@Test
	void findForReport_withEventAndDateRange_shouldReturnMatchingBookings() {
		// Given
		Booking booking = new Booking();
		booking.setUserId(1L);
		booking.setEventId(2L);
		booking.setShowId(3L);
		booking.setNumberOfSeats(2);
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setTotalAmount(BigDecimal.valueOf(100));
		booking.setGstAmount(BigDecimal.ZERO);
		booking.setDiscountAmount(BigDecimal.ZERO);
		booking.setBookingTime(LocalDateTime.now().minusHours(1));
		bookingRepository.save(booking);

		LocalDateTime from = LocalDateTime.now().minusDays(1);
		LocalDateTime to = LocalDateTime.now().plusDays(1);

		// When
		List<Booking> result = bookingRepository.findForReport(2L, from, to);

		// THEN
		assertThat(result).isNotEmpty();
		assertThat(result.get(0).getEventId()).isEqualTo(2L);
	}
}

