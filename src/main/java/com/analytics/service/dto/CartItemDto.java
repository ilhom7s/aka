package com.analytics.service.dto;

import com.analytics.service.entity.CartItem;

/**
 * DTO for {@link CartItem}
 */
public record CartItemDto(Long id, Long userId, Long variantId, Integer amount, ProductVariantDto productVariant) {
}