package com.analytics.service.dto;

import java.util.List;

public record CreateOrderRequest(
        List<RequestItems> list,
        String address,
        String phone
) {
}
