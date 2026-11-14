package com.analytics.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // Кто добавил

    @Column(name = "variant_id")
    private Long variantId; // Что именно добавил (ссылка на ProductVariant)

    private Integer quantity; // Сколько штук
    @ManyToOne
    @JoinColumn(name = "variant_id", insertable = false, updatable = false)
    private ProductVariant productVariant;


}