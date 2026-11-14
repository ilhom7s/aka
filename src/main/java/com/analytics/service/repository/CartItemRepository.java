package com.analytics.service.repository;


import com.analytics.service.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Найти всю корзину конкретного пользователя
    @EntityGraph(attributePaths = "productVariant")
    List<CartItem> findByUserId(Long userId);

    // Проверить, лежит ли уже конкретное кимоно в корзине этого пользователя
    Optional<CartItem> findByUserIdAndVariantId(Long userId, Long variantId);

    // Очистить корзину после оформления заказа
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    /** Убрать из корзины только оплаченный вариант. */
    @Modifying
    @Transactional
    void deleteByUserIdAndVariantId(Long userId, Long variantId);

    @EntityGraph(attributePaths = "productVariant")
    Optional<CartItem> findByIdAndUserId(Long id,Long userId);
}
