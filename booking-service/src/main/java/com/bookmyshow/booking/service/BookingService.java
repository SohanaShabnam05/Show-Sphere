package com.bookmyshow.booking.service;

import com.bookmyshow.booking.dto.BookingRequest;
import com.bookmyshow.booking.dto.CancelBookingResponse;
import com.bookmyshow.booking.entity.Booking;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

	Booking createBooking(Long userId, BookingRequest request);

	List<Booking> getMyBookings(Long userId);

	/**
	 * @param upcoming true = only upcoming (show start > now), false = only past, null = all
	 */
	List<Booking> getMyBookings(Long userId, Boolean upcoming);

	CancelBookingResponse cancelBooking(Long userId, Long bookingId, Integer seatsToCancel);
	boolean hasBookingsByShowId(Long showId);

	String getReportCsv(Long eventId, LocalDate fromDate, LocalDate toDate);
}
