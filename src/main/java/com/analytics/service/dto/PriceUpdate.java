package com.analytics.service.dto;

import java.math.BigDecimal;

public record PriceUpdate(
        Long id,
        String type,
        BigDecimal price
) {
}
