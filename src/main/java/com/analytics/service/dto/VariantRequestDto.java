package com.analytics.service.dto;

/** Запрос конкретного варианта: категория + цвет + размер. */
public record VariantRequestDto(
        String productId,
        String color,
        String size) {
}
