package com.analytics.service.dto;

import java.math.BigDecimal;
import java.util.List;

/** Один вариант товара (конкретный размер + цвет). available = есть ли в наличии (кол-во > 0). */
public record VariantDto(
        Long variantId,
        String size,
        String color,
        BigDecimal price,
        String imageUrl,
        List<ProductImageDto> images,
        boolean available) {
}
