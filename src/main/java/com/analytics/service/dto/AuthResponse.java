package com.analytics.service.dto;

public record AuthResponse(String token, String email, String role) {
}
