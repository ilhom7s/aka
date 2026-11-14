package com.analytics.service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "description", length = 1000)
    private String description;

    /** Обложка карточки = первое фото из галереи. Оставлено для витрины, которая читает imageUrl. */
    private String imageUrl;

    private String color;
    private String size;
    private BigDecimal price;
    @Column(name = "quantity",nullable = false)
    private Integer quantity;
    private Integer plotnost;
    private String material;

    /** Флаг «активен». false = архивный (мягкое удаление), в витрине и админке не показывается. */
    @Column(name = "active", columnDefinition = "boolean default true")
    private Boolean active = true;

    /** Галерея фото товара: минимум 3, максимум 5 (проверяется в сервисе). */
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @BatchSize(size = 50)
    private List<ProductImage> images = new ArrayList<>();

    /** Удобный метод: сохраняет двунаправленную связь в целостности. */
    public void addImage(ProductImage image) {
        image.setProductVariant(this);
        this.images.add(image);
    }

    public void clearImages() {
        this.images.clear();
    }
}
