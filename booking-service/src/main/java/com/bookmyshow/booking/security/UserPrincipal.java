package com.bookmyshow.booking.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Principal set by JwtFilter from JWT claims. Use in controllers via
 * (UserPrincipal) authentication.getPrincipal() and principal.getUserId().
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements Serializable {

	private Long userId;
	private String email;
}
