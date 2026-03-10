package com.bookmyshow.event.repository;

import com.bookmyshow.event.entity.Event;
import com.bookmyshow.event.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Event entity.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

	List<Event> findByCategory(EventCategory category);
}
