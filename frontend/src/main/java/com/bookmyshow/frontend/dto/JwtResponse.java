package com.bookmyshow.frontend.dto;

import lombok.Data;

@Data
public class JwtResponse {
	private String token;
	private String type;
	private Long userId;
	private String email;
	private String role;
}
