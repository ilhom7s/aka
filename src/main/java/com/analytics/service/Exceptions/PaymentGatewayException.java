package com.analytics.service.Exceptions;

/** Платёжный сервис недоступен или отклонил запрос → HTTP 502. */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) { super(message); }
}
