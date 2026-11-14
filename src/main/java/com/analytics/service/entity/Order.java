package com.analytics.service.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "user_id")
    private Long userId;

  /** Read-only связь на пользователя, чтобы показывать email в админке. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    private String address;
    private String phone;
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

  // PENDING / PAID / NEW / SHIPPED / DONE / CANCELLED
    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

  /** id платежа в ЮKassa. Нужен, чтобы сверить статус и не провести оплату дважды. */
    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "order",cascade = CascadeType.ALL,orphanRemoval = true)
    private Set<OrderItems> orderItems;
}
