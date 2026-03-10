package com.bookmyshow.auth.entity;

/**
 * User role enumeration for RBAC. Stored as EnumType.STRING in User entity.
 * Exactly one admin (seeded); all registrations get ROLE_USER.
 */
public enum Role {

	ROLE_USER,
	ROLE_ADMIN
}
