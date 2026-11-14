package com.analytics.service.dto;

import lombok.Builder;

@Builder
public record UserInfoResponseDto(
        String email,
        String phone
) {
}
