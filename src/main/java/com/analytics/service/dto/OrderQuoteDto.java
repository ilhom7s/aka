package com.analytics.service.dto;

import java.math.BigDecimal;

/** Расчёт суммы заказа, сделанный на сервере: товары + доставка = к оплате. */
public record OrderQuoteDto(
        BigDecimal goods,
        BigDecimal shipping,
        BigDecimal total) {
}
