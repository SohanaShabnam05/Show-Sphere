package com.bookmyshow.coupon.scheduler;

import com.bookmyshow.coupon.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponCleanupSchedulerTest {

	@Mock
	private CouponRepository couponRepository;

	@InjectMocks
	private CouponCleanupScheduler scheduler;

	@Test
	void cleanupExpiredOrConsumedCoupons_shouldInvokeRepositoryDeleteWithToday() {
		// Given
		when(couponRepository.deleteExpiredOrConsumed(any(LocalDate.class))).thenReturn(3);

		// When
		scheduler.cleanupExpiredOrConsumedCoupons();

		// THEN
		var captor = org.mockito.ArgumentCaptor.forClass(LocalDate.class);
		verify(couponRepository).deleteExpiredOrConsumed(captor.capture());
		assertThat(captor.getValue()).isEqualTo(LocalDate.now());
	}
}

