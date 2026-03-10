package com.bookmyshow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

	private String token;
	private String type;
	private Long userId;
	private String email;
	private String role;
}