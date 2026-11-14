package com.analytics.service.dto;

import java.time.YearMonth;

public record CardInfo(
        Long number,
        YearMonth dateOfExpires,
        Integer cvv2
) {
}
