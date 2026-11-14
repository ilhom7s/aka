package com.analytics.service.dto;

import java.math.BigDecimal;

/**
 * Текстовые поля товара, приходят как JSON-часть multipart-запроса.
 * Фото приходят отдельными файлами, поэтому imageUrl здесь больше нет.
 */
public record ProductRequestDto(
        String productId,
        String productName,
        String description,
        String color,
        String size,
        BigDecimal price,
        Integer quantity,
        Integer plotnost,
        String material) {
}
