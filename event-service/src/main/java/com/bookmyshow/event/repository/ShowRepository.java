package com.bookmyshow.event.repository;

import com.bookmyshow.event.entity.EventCategory;
import com.bookmyshow.event.entity.Show;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Show entity.
 */
@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

	@Query("SELECT DISTINCT s FROM Show s JOIN FETCH s.event JOIN FETCH s.theater")
	List<Show> findAllWithEventAndTheater();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Show s WHERE s.id = :id")
	Optional<Show> findByIdForUpdate(@Param("id") Long id);

	@Query("SELECT s FROM Show s WHERE s.theater.id = :theaterId AND s.event.id = :eventId " +
			"AND (s.startTime < :endTime AND s.endTime > :startTime)")
	List<Show> findOverlappingShows(@Param("theaterId") Long theaterId, @Param("eventId") Long eventId,
			@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

	List<Show> findByEventId(Long eventId);

	List<Show> findByTheaterId(Long theaterId);

	@Query("SELECT s FROM Show s JOIN FETCH s.event JOIN FETCH s.theater " +
			"WHERE (:category IS NULL OR s.event.category = :category) " +
			"AND (:showDate IS NULL OR cast(s.startTime as date) = :showDate) " +
			"AND (:theaterId IS NULL OR s.theater.id = :theaterId) " +
			"AND (:eventName IS NULL OR :eventName = '' OR LOWER(s.event.name) LIKE LOWER(CONCAT('%', :eventName, '%')))")
	List<Show> searchShows(@Param("category") EventCategory category, @Param("showDate") LocalDate showDate,
			@Param("theaterId") Long theaterId, @Param("eventName") String eventName);
}
