package com.analytics.service.dto;

import com.analytics.service.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for {@link Order}
 */
public record OrderDto(Long id, Long userId, String userEmail, String address, String phone, BigDecimal totalAmount,
                       String status, String paymentId, LocalDateTime createdAt, Set<OrderItemsDto> orderItems) {
}