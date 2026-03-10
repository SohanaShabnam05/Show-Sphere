package com.bookmyshow.coupon.scheduler;

import com.bookmyshow.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponCleanupScheduler {

	private final CouponRepository couponRepository;

	/**
	 * Runs every 10 minutes to delete expired or fully consumed coupons.
	 */
	@Scheduled(cron = "0 */10 * * * *")
	@Transactional
	public void cleanupExpiredOrConsumedCoupons() {

		LocalDate today = LocalDate.now();
		int deleted = couponRepository.deleteExpiredOrConsumed(today);
		if (deleted > 0) {
			log.info("Coupon cleanup scheduler removed {} expired/consumed coupons", deleted);
		}
	}
}

