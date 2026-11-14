package com.analytics.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class OrderItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "variant_id",insertable = false,updatable = false)
    private Long variantId;
    private BigDecimal amount;
    private Integer quantity;

    @ManyToOne
    @JoinColumn(name="variant_id")
    private ProductVariant productVariant;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    /** Сумма по строке = цена × количество. Используется в шаблонах. */
    @Transient
    public BigDecimal getLineTotal() {
        if (amount == null || quantity == null) return BigDecimal.ZERO;
        return amount.multiply(BigDecimal.valueOf(quantity));
    }
}
