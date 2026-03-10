package com.bookmyshow.auth.util;

import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.dto.RegisterRequest;
import com.bookmyshow.auth.exception.InvalidDobException;
import com.bookmyshow.auth.exception.InvalidPhoneException;
import com.bookmyshow.auth.exception.UnderAgeException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationValidatorTest {

	@Test
	void validateMobile_valid10Digit_shouldPass() {
		// When / THEN
		assertThatCode(() -> RegistrationValidator.validateMobile("1234567890"))
				.doesNotThrowAnyException();
	}

	@Test
	void validateMobile_invalidFormat_shouldThrowInvalidPhoneException() {
		// When / THEN
		assertThatThrownBy(() -> RegistrationValidator.validateMobile("123"))
				.isInstanceOf(InvalidPhoneException.class)
				.hasMessage(AuthConstants.ERROR_PHONE_MUST_BE_10_DIGITS);
	}

	@Test
	void validateDateOfBirth_null_shouldThrowInvalidDobException() {
		// When / THEN
		assertThatThrownBy(() -> RegistrationValidator.validateDateOfBirth(null))
				.isInstanceOf(InvalidDobException.class)
				.hasMessage(AuthConstants.ERROR_DOB_REQUIRED);
	}

	@Test
	void validateDateOfBirth_futureDate_shouldThrowInvalidDobException() {
		// Given
		LocalDate future = LocalDate.now().plusDays(1);

		// When / THEN
		assertThatThrownBy(() -> RegistrationValidator.validateDateOfBirth(future))
				.isInstanceOf(InvalidDobException.class)
				.hasMessage(AuthConstants.ERROR_DOB_INVALID);
	}

	@Test
	void validateMinimumAge_youngerThanAllowed_shouldThrowUnderAgeException() {
		// Given
		LocalDate tooYoung = LocalDate.now().minusYears(AuthConstants.MINIMUM_AGE_YEARS - 1);

		// When / THEN
		assertThatThrownBy(() -> RegistrationValidator.validateMinimumAge(tooYoung))
				.isInstanceOf(UnderAgeException.class)
				.hasMessage(AuthConstants.ERROR_UNDER_AGE);
	}

	@Test
	void validateRegistrationRequest_validData_shouldPassAllChecks() {
		// Given
		RegisterRequest request = RegisterRequest.builder()
				.mobile("1234567890")
				.dateOfBirth(LocalDate.now().minusYears(AuthConstants.MINIMUM_AGE_YEARS + 1))
				.build();

		// When / THEN
		assertThatCode(() -> RegistrationValidator.validateRegistrationRequest(request))
				.doesNotThrowAnyException();
	}
}

