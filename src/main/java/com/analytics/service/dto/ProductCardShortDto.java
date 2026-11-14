package com.analytics.service.dto;

import java.math.BigDecimal;
import java.util.List;

/** Короткая карточка для витрины/рекомендаций. */
public record ProductCardShortDto(
        String name,
        String category,
        BigDecimal priceFrom,
        List<ProductImageDto> images) {
}
