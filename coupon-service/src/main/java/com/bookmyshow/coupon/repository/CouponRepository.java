package com.bookmyshow.coupon.repository;

import com.bookmyshow.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

	Optional<Coupon> findByCode(String code);

	Optional<Coupon> findByCodeAndValidFromLessThanEqualAndValidToGreaterThanEqual(
			String code, LocalDate atStartOfDay, LocalDate atEndOfDay);

	@Modifying
	@Query("DELETE FROM Coupon c WHERE c.validTo < :today OR (c.maxRedemptions IS NOT NULL AND c.redemptionCount >= c.maxRedemptions)")
	int deleteExpiredOrConsumed(@Param("today") LocalDate today);
}
