package com.bookmyshow.frontend.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Data
public class RegisterRequest {
	private String name;
	private String email;
	private String password;
	private String mobile;
	private String address;
	@DateTimeFormat(iso = DATE)
	private LocalDate dateOfBirth;
}
