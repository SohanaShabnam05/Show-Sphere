package com.bookmyshow.auth.dto;

import com.bookmyshow.auth.entity.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class UserSummaryResponse {

	private Long id;
	private String name;
	private String email;
	private String mobile;
	private String address;
	private LocalDate dateOfBirth;
	private Role role;
}

