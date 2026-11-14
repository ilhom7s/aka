package com.analytics.service.dto;

public record QuantityUpdate(
        String type,
        Long id,
        Integer quantity
) {
}
