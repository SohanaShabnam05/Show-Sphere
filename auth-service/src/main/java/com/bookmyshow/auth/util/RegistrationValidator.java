package com.bookmyshow.auth.util;

import com.bookmyshow.auth.constant.AuthConstants;
import com.bookmyshow.auth.dto.RegisterRequest;
import com.bookmyshow.auth.exception.InvalidDobException;
import com.bookmyshow.auth.exception.InvalidPhoneException;
import com.bookmyshow.auth.exception.UnderAgeException;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validates registration request and throws custom exceptions for each validation failure.
 */
@UtilityClass
public class RegistrationValidator {

	private static final Pattern TEN_DIGITS = Pattern.compile(AuthConstants.REGEX_10_DIGITS);

	/**
	 * Validates mobile number is exactly 10 digits. Throws {@link InvalidPhoneException} if not.
	 *
	 * @param mobile mobile number to validate
	 */
	public static void validateMobile(String mobile) {

		if (Objects.isNull(mobile) || !TEN_DIGITS.matcher(mobile.trim()).matches()) {
			throw new InvalidPhoneException(AuthConstants.ERROR_PHONE_MUST_BE_10_DIGITS);
		}
	}

	/**
	 * Validates date of birth is present and in the past. Throws {@link InvalidDobException} if not.
	 *
	 * @param dateOfBirth date of birth to validate
	 */
	public static void validateDateOfBirth(LocalDate dateOfBirth) {

		if (Objects.isNull(dateOfBirth)) {
			throw new InvalidDobException(AuthConstants.ERROR_DOB_REQUIRED);
		}
		if (!dateOfBirth.isBefore(LocalDate.now())) {
			throw new InvalidDobException(AuthConstants.ERROR_DOB_INVALID);
		}
	}

	/**
	 * Validates user is at least 18 years old based on DOB. Throws {@link UnderAgeException} if not.
	 *
	 * @param dateOfBirth date of birth
	 */
	public static void validateMinimumAge(LocalDate dateOfBirth) {

		if (Objects.isNull(dateOfBirth)) {
			throw new InvalidDobException(AuthConstants.ERROR_DOB_REQUIRED);
		}
		int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
		if (age < AuthConstants.MINIMUM_AGE_YEARS) {
			throw new UnderAgeException(AuthConstants.ERROR_UNDER_AGE);
		}
	}

	/**
	 * Runs all registration validations (mobile, DOB, minimum age). Email duplicate check is done separately.
	 *
	 * @param request registration request
	 */
	public static void validateRegistrationRequest(RegisterRequest request) {

		validateMobile(request.getMobile());
		validateDateOfBirth(request.getDateOfBirth());
		validateMinimumAge(request.getDateOfBirth());
	}
}
