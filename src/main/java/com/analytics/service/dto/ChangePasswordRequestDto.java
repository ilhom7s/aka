package com.analytics.service.dto;

public record ChangePasswordRequestDto(
        String password,
        String newPassword,
        String repeatNewPassword
) {
}
