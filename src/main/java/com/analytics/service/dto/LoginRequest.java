package com.analytics.service.dto;

/** login — это либо email, либо номер телефона. */
public record LoginRequest(String login, String password) {
}
