package com.analytics.service.dto;

/** Ответ фронту после оформления: номер заказа и ссылка на оплату. */
public record PaymentResponse(
        Long orderId,
        String confirmationUrl) {
}
