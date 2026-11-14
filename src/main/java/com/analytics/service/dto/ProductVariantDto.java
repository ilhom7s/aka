package com.analytics.service.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for {@link com.analytics.service.entity.ProductVariant}
 */
public record ProductVariantDto(
        Long id,
        String productId,
        String productName,
        String description,
        String imageUrl,
        String color,
        String size,
        BigDecimal price,
        Integer quantity,
        Integer plotnost,
        String material,
        List<ProductImageDto> images) {
}
