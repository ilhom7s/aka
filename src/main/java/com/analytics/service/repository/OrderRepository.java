package com.analytics.service.repository;


import com.analytics.service.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Заказы пользователя вместе с позициями, товарами и пользователем — одним запросом. */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.productVariant " +
           "LEFT JOIN FETCH o.user " +
           "WHERE o.userId = :userId ORDER BY o.id DESC")
    List<Order> findUserOrders(@Param("userId") Long userId);

    /** Все заказы для админки — с позициями, товарами и email пользователя. */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.productVariant " +
           "LEFT JOIN FETCH o.user " +
           "ORDER BY o.id DESC")
    List<Order> findAllWithItems();

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.productVariant " +
           "LEFT JOIN FETCH o.user " +
           "WHERE o.status <> 'DELIVERED' ORDER BY o.id DESC")
    List<Order> findAllNotDeliveredOrders();

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.productVariant " +
           "LEFT JOIN FETCH o.user " +
           "WHERE o.status = 'DELIVERED' ORDER BY o.id DESC")
    List<Order> findAllDeliveredOrders();

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.productVariant " +
            "LEFT JOIN FETCH o.user " +
            "WHERE o.userId = :userId AND o.status IN (com.analytics.service.entity.OrderStatus.PENDING_PAYMENT, com.analytics.service.entity.OrderStatus.PAID) " +
            "ORDER BY o.id DESC")
    List<Order> findActiveUserOrders(Long userId);



    /** Заказ с блокировкой строки: webhook и опрос статуса не обработают оплату дважды. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);


}
