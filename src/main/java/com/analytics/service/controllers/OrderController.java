package com.analytics.service.controllers;

import com.analytics.service.dto.CreateOrderRequest;

import com.analytics.service.services.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;


    /** Оформить заказ и получить ссылку на оплату. */
    @PostMapping
    public void createOrder(@RequestBody CreateOrderRequest request, Authentication authentication) {
         orderService.createOrder(request, authentication.getName());
    }


}
