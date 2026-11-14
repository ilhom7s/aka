package com.analytics.service.services;

import com.analytics.service.Exceptions.NotFoundException;

import com.analytics.service.dto.*;

import com.analytics.service.entity.*;
import com.analytics.service.repository.OrderRepository;
import com.analytics.service.repository.ProductVariantRepository;
import com.analytics.service.repository.UserMapper;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class OrderService {
    private final UserMapper userMapper;
    private final UserService userService;

    private final ProductVariantRepository variantRepository;

    private final OrderRepository orderRepository;


    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, String login){
        User user = userService.findUserByEmailOrPhone(login);
        if (request.address() == null || request.address().isBlank())
            throw new IllegalArgumentException("Укажите адрес доставки");
        if (request.phone() == null || request.phone().isBlank())
            throw new IllegalArgumentException("Укажите телефон");
        Map<Long, Integer> grouped = groupItems(request.list());

        Order order = new Order();
        order.setUserId(user.getId());
        order.setAddress(request.address().trim());
        order.setPhone(request.phone().trim());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        Set<OrderItems> items = new HashSet<>();
        BigDecimal goods = BigDecimal.ZERO;

        // блокируем варианты в порядке id — одинаковый порядок у всех потоков исключает взаимную блокировку
        for (Map.Entry<Long, Integer> e : grouped.entrySet()) {
            ProductVariant v = variantRepository.findByIdForUpdate(e.getKey())
                    .orElseThrow(() -> new NotFoundException("Товар не найден"));
            int qty = e.getValue();

            if (Boolean.FALSE.equals(v.getActive()))
                throw new IllegalArgumentException("Товар снят с продажи: " + v.getProductName());
            if (v.getQuantity() == null || v.getQuantity() < qty)
                throw new IllegalArgumentException("Недостаточно товара: " + v.getProductName());

            v.setQuantity(v.getQuantity() - qty);                 // резерв

            OrderItems item = new OrderItems();
            item.setOrder(order);
            item.setProductVariant(v);
            item.setQuantity(qty);
            item.setAmount(v.getPrice());                         // цена из БД, не от клиента
            items.add(item);

            goods = goods.add(v.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        order.setOrderItems(items);
        order.setTotalAmount(goods.add(shippingFor(goods)));
        // доставка входит в сумму оплаты
        return userMapper.toOrderDto(orderRepository.save(order));
    }
    private Map<Long, Integer> groupItems(List<RequestItems> list) {
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("Список товаров пуст");
        Map<Long, Integer> grouped = new TreeMap<>();
        for (RequestItems it : list) {
            if (it.variantId() == null) throw new IllegalArgumentException("Не указан товар");
            if (it.quantity() == null || it.quantity() < 1) throw new IllegalArgumentException("Некорректное количество");
            grouped.merge(it.variantId(), it.quantity(), Integer::sum);
        }
        return grouped;
    }
    public BigDecimal shippingFor(BigDecimal goods){
        return BigDecimal.ZERO;
    }





}
