package com.analytics.service.dto;

public record ChangeEmailRequestDto(
        String newEmail,
        String password
) {
}
