package com.bookmyshow.event.repository;

import com.bookmyshow.event.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for Theater entity.
 */
@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {
}
