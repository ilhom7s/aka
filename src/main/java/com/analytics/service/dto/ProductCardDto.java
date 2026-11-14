package com.analytics.service.dto;

import java.math.BigDecimal;
import java.util.List;

/** Карточка товара: общая инфа + все варианты (размеры/цвета). */
public record ProductCardDto(
        String name,
        String category,
        String description,
        String material,
        Integer plotnost,
        BigDecimal priceFrom,
        List<String> sizes,
        List<String> colors,
        List<ProductImageDto> images,
        List<VariantDto> variants) {
}
