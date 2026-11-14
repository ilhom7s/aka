package com.analytics.service.dto;

import com.analytics.service.entity.OrderItems;

import java.math.BigDecimal;

/**
 * DTO for {@link OrderItems}
 */
public record OrderItemsDto(Long id, Long variantId, BigDecimal amount, Integer quantity,
                            ProductVariantDto productVariant, BigDecimal lineTotal) {
}