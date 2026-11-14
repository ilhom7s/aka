package com.analytics.service.dto;

/**
 * DTO for {@link com.analytics.service.entity.ProductImage}
 */
public record ProductImageDto(
        Long id,
        String imageUrl,
        Integer position) {
}
