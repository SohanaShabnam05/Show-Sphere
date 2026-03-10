package com.bookmyshow.booking.repository;

import com.bookmyshow.booking.entity.Booking;
import com.bookmyshow.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

	List<Booking> findByUserIdOrderByBookingTimeDesc(Long userId);

	List<Booking> findByUserIdAndStatusOrderByBookingTimeDesc(Long userId, BookingStatus status);

	long countByUserIdAndStatus(Long userId, BookingStatus status);

	List<Booking> findByShowId(Long showId);

	List<Booking> findByStatus(BookingStatus status);

	boolean existsByShowId(Long showId);

	long countByUserIdAndStatusNot(Long userId, BookingStatus status);

	@Query("""
			SELECT b FROM Booking b
			WHERE (:eventId IS NULL OR b.eventId = :eventId)
			  AND (:from IS NULL OR b.bookingTime >= :from)
			  AND (:to IS NULL OR b.bookingTime <= :to)
			""")
	List<Booking> findForReport(@Param("eventId") Long eventId,
	                            @Param("from") LocalDateTime from,
	                            @Param("to") LocalDateTime to);
}
