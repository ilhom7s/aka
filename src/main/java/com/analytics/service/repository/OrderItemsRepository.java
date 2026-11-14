package com.analytics.service.repository;

import com.analytics.service.entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

    /** Есть ли позиции заказов, ссылающиеся на данный вариант товара. */
    boolean existsByVariantId(Long variantId);


}
