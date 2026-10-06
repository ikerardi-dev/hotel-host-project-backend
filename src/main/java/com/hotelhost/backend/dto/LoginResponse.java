package com.hotelhost.backend.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserResponse user) {
}
